package controllers;

import Util.APIResponse;
import Util.Message;
import Util.ReferralChain;
import actors.HelperActor;
import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import drools.ActorMessage;
import models.AgentData;
import models.Neighbor;
import play.mvc.*;
import plugins.Drools;
import scala.concurrent.Await;
import scala.concurrent.Future;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {
    @Inject
    Drools drools;

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }

    public Result loadGraph(){
        //KieContainer kc = kieServices.getKieClasspathContainer(environment.classLoader());
        String JSON=request().body().asJson().toString();
        System.out.println(JSON);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            AgentData[] agentList = objectMapper.readValue(JSON, AgentData[].class);
            ActorStart.initialiseSystem(agentList);
            for(int i=0;i<agentList.length;i++){
                System.out.println(agentList[i].toString());
            }
        } catch (IOException e) {
            return created(APIResponse.createResponse(e.getMessage(), false));
        }

        ActorRef helper=ActorStart.actorSystem.actorOf(HelperActor.props(),"Helper");
        helper.tell(new Message.GenerateAndExecute(),ActorRef.noSender());
        return created(APIResponse.createResponse("", true));

    }

    public Result getAnswer(String actor,String values){
        double[] query=new double[4];
        final Timeout timeout = new Timeout(50, TimeUnit.SECONDS);
        String[] val=values.split(",") ;
        try {
            if(val.length>4) throw new Exception();
            for (int i = 0; i < 4; i++) {
                query[i] = Double.parseDouble(val[i]);
            }
        }catch (Exception e){
            return created(APIResponse.createResponse("Please enter the query in xx,xx,xx,xx format", false));
        }
        if(!ActorStart.ActorMap.containsKey(actor)){
            System.out.println("Unable to find Actor " +actor +" in the system");
            return created(APIResponse.createResponse("Unable to find Actor " +actor +" in the system", false));
        }
        //System.out.println(Arrays.toString(query));
        ReferralChain.next=new Stack<>();
        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","UserAgent","SEND", "Query","Sending Query for "+ Arrays.toString(query)+" to " +actor));
        drools.kieSession.fireAllRules();
        Future<Object> future = Patterns.ask(ActorStart.ActorMap.get(actor),new Message.Query("UserAgent",actor,query,0),timeout);
        Message.Response result = null;
        try {
            Object o=Await.result(future, timeout.duration());
            if(o instanceof Message.Response) {
                result=(Message.Response) o;
                if(result.gotAnswer){
                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","UserAgent","RECV","Answer","Recieved Answer for "+ Arrays.toString(query)+ " as " +Arrays.toString(result.ans)));
                    drools.kieSession.fireAllRules();
                    ReferralChain.refChain = new LinkedHashSet<>();
                    return created(APIResponse.createResponseWithAnswer(true,result.ans));
                }else if(ReferralChain.next.empty()){
                    ReferralChain.refChain = new LinkedHashSet<>();
                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","UserAgent","RECV","Reject","Unable to find answer to this query"));
                    drools.kieSession.fireAllRules();
                    return created(APIResponse.createResponse("Unable to find answer to this query", false));
                }else{
                    while(!ReferralChain.next.empty()) {
                        Neighbor temp = ReferralChain.next.pop();
                        while (ReferralChain.refChain.contains(temp.getName())) {
                            temp = ReferralChain.next.pop();
                        }
                            //System.out.println("Popped from Stack " + temp.getName());
                            drools.kieSession.insert(new ActorMessage(System.currentTimeMillis() + "", actor, "SEND", "Query", "Sending Query for " + Arrays.toString(query) + " to " + temp.getName()));
                            drools.kieSession.fireAllRules();
                            future = Patterns.ask(ActorStart.ActorMap.get(temp.getName()), new Message.Query(actor, temp.getName(), query, 1), timeout);
                            o = Await.result(future, timeout.duration());
                            if (o instanceof Message.Response) {
                                result = (Message.Response) o;
                                if (result.gotAnswer) {
                                    int i = 1;
                                    for (String s : ReferralChain.refChain) {
                                        ActorStart.ActorMap.get(actor).tell(new Message.UpdateSociability(s, query, result.ans, i), ActorRef.noSender());
                                        System.out.println("Refferal Chain element" + s);
                                    }
                                    ReferralChain.refChain = new LinkedHashSet<>();
                                    ActorStart.ActorMap.get(actor).tell(new Message.UpdateNeighbours(), ActorRef.noSender());
                                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis() + "", "UserAgent", "RECV", "Answer", "Recieved Answer for " + Arrays.toString(query) + " as " + Arrays.toString(result.ans)));
                                    drools.kieSession.fireAllRules();
                                    return created(APIResponse.createResponseWithAnswer(true, result.ans));
                                }
                            }


                    }
                }
            }
        } catch (Exception e) {
            drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","UserAgent","RECV","Reject","Unable to find answer to this query"));
            drools.kieSession.fireAllRules();
            ReferralChain.refChain = new LinkedHashSet<>();
            return created(APIResponse.createResponse(e.getMessage(), false));
        }

        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","UserAgent","RECV","Reject","Unable to find answer to this query"));
        drools.kieSession.fireAllRules();
        ReferralChain.refChain = new LinkedHashSet<>();
        return created(APIResponse.createResponse("Unable to find answer to this query", false));

    }

    public Result dumpStates(String actor){
        if(ActorStart.ActorMap.containsKey(actor)){
            final Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
            Future<Object> future = Patterns.ask(ActorStart.ActorMap.get(actor),new Message.GetNeighboursAndAcquaintances(),timeout);
            Message.NeighboursAndAcquaintances result = null;
            try{
                result=(Message.NeighboursAndAcquaintances)Await.result(future, timeout.duration());
                return created(APIResponse.createResponseForDump(true,result.neighbors,result.acquaintances));
            }catch (Exception e){
                return created(APIResponse.createResponse("Actor "+ actor +" does not exist in the system"+e.getMessage(), false));
            }

        }
        return created(APIResponse.createResponse("Actor "+ actor +" does not exist in the system", false));
    }

    public Result getLogs() throws IOException {
        String FILENAME = "logs/droolsLog.log";
        File file = new File(FILENAME);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        return created(data);
    }

}
