package actors;

import Util.APIResponse;
import Util.Message;
import Util.ReferralChain;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import controllers.ActorStart;
import controllers.HomeController;
import drools.ActorMessage;
import models.Neighbor;
import plugins.Drools;
import referral_helper.QueryGenerator;
import scala.concurrent.Await;
import scala.concurrent.Future;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class HelperActor extends AbstractActor{

    static public Props props() {
        return Props.create(HelperActor.class, () -> new HelperActor());
    }
    @Inject
    public HelperActor(){
        System.out.println("Creating Helper Actor");
    }

    @Inject
    Drools drools;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Message.GenerateAndExecute.class, gae -> {
                    System.out.println("Printing actors sequentially");
                    QueryGenerator queryGenerator = QueryGenerator.getInstance();
                    final Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
                    for(String a: ActorStart.ActorMap.keySet()){
                        System.out.println("Queries for Actor => "+ a);
                        ActorRef ref=ActorStart.ActorMap.get(a);
                        Future<Object> future = Patterns.ask(ref, new Message.GetNeeds(), timeout);
                        Message.Needs result1=null;
                        try {
                            result1=(Message.Needs) Await.result(future, timeout.duration());
                        }catch (Exception e){

                        }
                        final double[] needs=result1.needs;
                        for(int i=0;i<25;i++){
                            double[] query=queryGenerator.genQuery(a,needs);
                            //ActorStart.ActorMap.get(a).tell(new Message.SetNeeds(query),ActorRef.noSender());
                            System.out.println("Query " + Arrays.toString(query));
                            ReferralChain.next=new Stack<>();
                            boolean done=false;
                            drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","QueryGenerator","SEND", "Query","Sending Query for "+ Arrays.toString(query)+" to " +a));
                            drools.kieSession.fireAllRules();
                            future = Patterns.ask(ref,new Message.Query("QueryGenerator",a,query,0),timeout);
                            Message.Response result = null;
                            try {
                                Object o=Await.result(future, timeout.duration());
                                if(o instanceof Message.Response) {
                                    result=(Message.Response) o;
                                    if(result.gotAnswer){
                                        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","QueryGenerator","RECV","Answer","Recieved Answer for "+ Arrays.toString(query)+ " as " +Arrays.toString(result.ans)));
                                        drools.kieSession.fireAllRules();
                                        System.out.println("Answer = " + Arrays.toString(result.ans));
                                        continue;
                                        //return created(APIResponse.createResponseWithAnswer(true,result.ans));
                                    }else if(ReferralChain.next.empty()){
                                        ReferralChain.refChain = new LinkedHashSet<>();
                                        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","QueryGenerator","RECV","Reject","Unable to find answer to this query"));
                                        drools.kieSession.fireAllRules();
                                        System.out.println("Unable to find answer to this query");
                                        //return created(APIResponse.createResponse("Unable to find answer to this query", false));
                                    }else{
                                        while(!ReferralChain.next.empty() && !done){
                                            Neighbor temp=ReferralChain.next.pop();
                                            while(ReferralChain.refChain.contains(temp.getName())) {
                                                temp=ReferralChain.next.pop();
                                            }
                                                //System.out.println("Popped from Stack " + temp.getName());
                                                drools.kieSession.insert(new ActorMessage(System.currentTimeMillis() + "", a, "SEND", "Query", "Sending Query for " + Arrays.toString(query) + " to " + temp.getName()));
                                                drools.kieSession.fireAllRules();
                                                future = Patterns.ask(ActorStart.ActorMap.get(temp.getName()), new Message.Query(a, temp.getName(), query, 1), timeout);
                                                o = Await.result(future, timeout.duration());
                                                if (o instanceof Message.Response) {
                                                    result = (Message.Response) o;
                                                    if (result.gotAnswer) {
                                                        int k = 1;
                                                        for (String s : ReferralChain.refChain) {
                                                            ActorStart.ActorMap.get(a).tell(new Message.UpdateSociability(s, query, result.ans, k), ActorRef.noSender());
                                                            System.out.println("Refferal Chain element" + s);
                                                        }
                                                        ReferralChain.refChain = new LinkedHashSet<>();
                                                        ActorStart.ActorMap.get(a).tell(new Message.UpdateNeighbours(), ActorRef.noSender());
                                                        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis() + "", "QueryGenerator", "RECV", "Answer", "Recieved Answer for " + Arrays.toString(query) + " as " + Arrays.toString(result.ans)));
                                                        drools.kieSession.fireAllRules();
                                                        System.out.println("Answer = " + Arrays.toString(result.ans));
                                                        done = true;
                                                        //return created(APIResponse.createResponseWithAnswer(true,result.ans));
                                                    }
                                                }


                                        }
                                    }
                                }
                            } catch (Exception e) {
                                ReferralChain.refChain = new LinkedHashSet<>();
                                drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"","QueryGenerator","RECV","Reject","Unable to find answer to this query"));
                                drools.kieSession.fireAllRules();
                                System.out.println("Unable to find answer to this query"+ e.getMessage());
                                //return created(APIResponse.createResponse(e.getMessage(), false));
                            }
                            //System.out.println(Arrays.toString(query));
                            ReferralChain.refChain = new LinkedHashSet<>();
                        }
                    }

                })
                .build();
    }
}
