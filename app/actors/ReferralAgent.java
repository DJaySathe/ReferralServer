package actors;

import Util.Message;
import Util.ReferralChain;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import controllers.ActorStart;
import drools.ActorMessage;
import models.AgentData;
import models.Neighbor;
import org.drools.compiler.kie.builder.impl.KieServicesImpl;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import plugins.Drools;
import referral_helper.Utils;
import scala.concurrent.Await;
import scala.concurrent.Future;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ReferralAgent extends AbstractActor {
    @Inject
    Drools drools;

    static public Props props(AgentData data) {
        return Props.create(ReferralAgent.class, () -> new ReferralAgent(data));
    }
    String name;
    double[] expertise;
    double[] needs;
    ArrayList<Neighbor> neighbors;
    HashMap<String,Neighbor> acquaintances;
    double[] currQuery;
    @Inject
    public ReferralAgent(AgentData data){
        name=data.getName();
        expertise=data.getExpertise();
        needs=data.getNeeds();
        neighbors=new ArrayList<>();
        acquaintances=new HashMap<>();
        for(Neighbor n:data.getNeighbors()){
            acquaintances.put(n.getName(),n);
            neighbors.add(n);
        }
        System.out.println("Creating Actor "+name);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Message.Query.class, query -> {
                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"RECV", "Query","Recieved Query "+ Arrays.toString(query.query)));
                    drools.kieSession.fireAllRules();
                    System.out.println("Query Recieved from " + query.sender + " to " + query.Reciever + " Depth " + query.depth);
                    Timeout timeout = new Timeout(25, TimeUnit.SECONDS);
                    if(Utils.isExpertiseMatch(expertise,query.query) && !query.sender.equals("QueryGenerator")){
                        System.out.println("Query Solved By " + name + " Sending back ans ");
                        if(query.depth!=0)
                            ActorStart.ActorMap.get(query.sender).tell(new Message.UpdateExpertise(query.Reciever,query.query,Utils.genAnswer(expertise,query.query)),getSender());
                        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Answer","Sending Answer for "+ Arrays.toString(query.query)+ " as " +Arrays.toString(Utils.genAnswer(expertise,query.query))));
                        drools.kieSession.fireAllRules();
                        getSender().tell(new Message.Response(query.Reciever,query.sender,Utils.genAnswer(expertise,query.query),true), getSender());

                    }else{
                        timeout = new Timeout(5, TimeUnit.SECONDS);
                        System.out.println(name+ " Cannot Answer the Query ");
                        ArrayList<Neighbor> neighbourList=new ArrayList<>();
                        for(Neighbor n:neighbors){
                            if(query.depth!=0) {
                                if (Utils.isExpertiseMatch(n.getExpertise(), query.query) || Utils.isExpertiseMatch(n.getSociability(), query.query)) {
                                    neighbourList.add(n);
                                }
                            }else{
                                neighbourList.add(n);
                            }
                        }
                        currQuery=query.query;
                        if(neighbourList.size()==0){
                            ReferralChain.refChain = new LinkedHashSet<>();
                            drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Reject","Sending Reject for "+ Arrays.toString(query.query)));
                            drools.kieSession.fireAllRules();
                            getSender().tell(new Message.Response(name,query.sender,null,false),ActorRef.noSender());//////////////////////
                            return;
                        }
                        Collections.sort(neighbourList, new Comparator<Neighbor>() {
                            @Override
                            public int compare(Neighbor o1, Neighbor o2) {
                                double s1=score(o1);
                                double s2=score(o2);
                                if(s1>s2){
                                    return 1;
                                }else if(s1<s2){
                                    return -1;
                                }else{
                                    return 0;
                                }
                            }
                            public double score(Neighbor o){
                                double e=0;
                                for(int i=0;i<4;i++){
                                    e+=currQuery[0]*o.getExpertise()[0];
                                }
                                double s=0;
                                for(int i=0;i<4;i++){
                                    s+=currQuery[0]*o.getSociability()[0];
                                }
                                return Utils.getWeightOfSociability()*s+(1-Utils.getWeightOfSociability())*e;
                            }
                        });

                        int i=0;
                        Boolean found=false;
                        if(query.depth==0) {
                                    for (Neighbor n : neighbourList) {
                                        ReferralChain.next.push(n);
                                    }
                                    if(!ReferralChain.next.empty()) {
                                        Neighbor n = ReferralChain.next.pop();
                                        //System.out.println("Popped from Stack " + n.getName());
                                        i++;
                                        System.out.println("Processing Neighbour " + n.getName());
                                        timeout = new Timeout(40, TimeUnit.SECONDS);
                                        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Query","Sending Query for "+ Arrays.toString(query.query)+" to " +n.getName()));
                                        drools.kieSession.fireAllRules();
                                        Future<Object> future = Patterns.ask(ActorStart.ActorMap.get(n.getName()), new Message.Query(name, n.getName(), query.query, query.depth + 1), timeout);
                                        Message.Response result = null;
                                        try {
                                            Object o = Await.result(future, timeout.duration());
                                            if (o instanceof Message.Response) {
                                                result = (Message.Response) o;
                                                if (result.gotAnswer) {
                                                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"RECV", "Answer","Recieved Answer for "+ Arrays.toString(query.query)+ " as " +Arrays.toString(result.ans)));
                                                    drools.kieSession.fireAllRules();
                                                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Answer","Sending Answer for "+ Arrays.toString(query.query)+" as " +Arrays.toString(result.ans)));
                                                    drools.kieSession.fireAllRules();
                                                    getSender().tell(result, getSender());
                                                    System.out.println("Sending back results");
                                                    found = true;
                                                } else {
                                                    ReferralChain.refChain = new LinkedHashSet<>();
                                                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Reject","Sending Reject for "+ Arrays.toString(query.query)));
                                                    drools.kieSession.fireAllRules();
                                                    getSender().tell(new Message.Response(name, query.sender, null, false), getSender());
                                                }
                                            } else if (o instanceof Message.Referral) {
                                                //drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Reject","Sending Reject for "+ Arrays.toString(query.query)));
                                                //drools.kieSession.fireAllRules();
                                                getSender().tell(new Message.Response(name, query.sender, null, false), getSender());
                                            }
                                        } catch (Exception e) {
                                            System.out.println("Processing Neighbour " + n.getName() + " " + e.getMessage());
                                        }
                                    }else{
                                        ReferralChain.refChain = new LinkedHashSet<>();
                                        drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Reject","Sending Reject for "+ Arrays.toString(query.query)));
                                        drools.kieSession.fireAllRules();
                                        getSender().tell(new Message.Response(name, query.sender, null, false), getSender());
                                    }


                        } else if(neighbourList.size()>0){
                                if(!neighbourList.get(0).getName().equals(query.sender)) {
                                    System.out.println("Processing Neighbour of " + name + " for referral " + neighbourList.get(0).getName());
                                    ReferralChain.refChain.add(name);
                                    Neighbor temp = new Neighbor();
                                    temp.setName(neighbourList.get(0).getName());
                                    //System.out.println("Pushed to Stack" + neighbourList.get(0).getName());
                                    ReferralChain.next.push(temp);
                                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Referral","Sending Referral for "+ Arrays.toString(query.query)+" as " +neighbourList.get(0).getName()));
                                    drools.kieSession.fireAllRules();
                                    getSender().tell(new Message.Referral(name, query.sender, neighbourList.get(0).getName(), query.query), getSender());
                                    ActorStart.ActorMap.get(query.sender).tell(new Message.AddAcquaintances(neighbourList.get(0).getName()), getSender());
                                }else{
                                    ReferralChain.refChain = new LinkedHashSet<>();
                                    drools.kieSession.insert(new ActorMessage(System.currentTimeMillis()+"",name,"SEND", "Reject","Sending Reject for "+ Arrays.toString(query.query)));
                                    drools.kieSession.fireAllRules();
                                    getSender().tell(new Message.Response(name,query.sender,null,false),getSender());
                                }
                        }
                    }


                })
                .match(Message.Referral.class, referral -> {
                    System.out.println(name + " recieved Referral Recieved from " + referral.sender + " to " + referral.Reciever +" for "+referral.ReferredActor);
                    if(!name.equals(referral.ReferredActor)) {
                        ReferralChain.refChain.add(referral.ReferredActor);
                        final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
                        Future<Object> future = Patterns.ask(ActorStart.ActorMap.get(referral.ReferredActor), new Message.Query(name, referral.ReferredActor, referral.query, 1), timeout);
                        Message.Response result = null;
                        try {
                            result = (Message.Response) Await.result(future, timeout.duration());
                            if (result.gotAnswer) {
                                for (String s : ReferralChain.refChain) {
                                    System.out.println("Refferal Chain element" + s);
                                }
                                //System.out.println(getSender().toString());
                                getSender().tell(new Message.Response(name,referral.sender,result.ans,true), getSender());
                            } else {
                                ReferralChain.refChain = new LinkedHashSet<>();
                                getSender().tell(new Message.Response(name,referral.sender,null,false), getSender());
                            }
                        } catch (Exception e) {
                            System.out.println("Processing  referral "+referral.ReferredActor+ " "+e.getMessage());
                        }
                    }else{
                        ReferralChain.refChain = new LinkedHashSet<>();
                        getSender().tell(new Message.Response(name,referral.sender,null,false), getSender());
                    }
                })
                .match(Message.Response.class, response -> {
                    System.out.println("Response Recieved by " + name);
                })
                .match(Message.GetNeeds.class, getNeeds -> {
                    getSender().tell(new Message.Needs(needs), getSender());
                })
                .match(Message.SetNeeds.class, setNeeds -> {
                    needs=setNeeds.needs;
                })
                .match(Message.UpdateExpertise.class, updateExpertise -> {
//                    if(!acquaintances.containsKey(updateExpertise.ref)) {
//                        Neighbor temp = new Neighbor();
//                        temp.setName(updateExpertise.ref);
//                        temp.setExpertise(new double[]{0.5, 0.5, 0.5, 0.5});
//                        temp.setSociability(new double[]{0.5, 0.5, 0.5, 0.5});
//                        acquaintances.put(updateExpertise.ref, temp);
//                    }
                    if(acquaintances.containsKey(updateExpertise.ref))
                        Utils.updateExpertise(updateExpertise.query,updateExpertise.ans,acquaintances.get(updateExpertise.ref).getExpertise());
                })
                .match(Message.UpdateSociability.class, updateSociability -> {
//                    if(!acquaintances.containsKey(updateSociability.ref)) {
//                        Neighbor temp = new Neighbor();
//                        temp.setName(updateSociability.ref);
//                        temp.setExpertise(new double[]{0.5, 0.5, 0.5, 0.5});
//                        temp.setSociability(new double[]{0.5, 0.5, 0.5, 0.5});
//                        acquaintances.put(updateSociability.ref, temp);
//                    }
                    if(acquaintances.containsKey(updateSociability.ref))
                        Utils.updateSociability(updateSociability.query,updateSociability.ans,updateSociability.dist,acquaintances.get(updateSociability.ref).getExpertise());
                })
                .match(Message.UpdateNeighbours.class, updateNeighbours -> {
                    double[] dummy=new double[]{1.0, 1.0, 1.0, 1.0};
                    ArrayList<Neighbor> al=new ArrayList<>();
                    for(String s:acquaintances.keySet()){
                        al.add(acquaintances.get(s));
                    }
                    Collections.sort(al, new Comparator<Neighbor>() {
                        @Override
                        public int compare(Neighbor o1, Neighbor o2) {
                            double s1=score(o1);
                            //System.out.println(o1.getName()+"  => "+s1);
                            double s2=score(o2);
                            //System.out.println(o2.getName()+"  => "+s2);
                            if(s1<s2){
                                return 1;
                            }else if(s1>s2){
                                return -1;
                            }else{
                                return 0;
                            }
                        }
                        public double score(Neighbor o){
                            double e=0;
                            for(int i=0;i<4;i++){
                                e+=dummy[0]*o.getExpertise()[0];
                            }
                            double s=0;
                            for(int i=0;i<4;i++){
                                s+=dummy[0]*o.getSociability()[0];
                            }
                            return Utils.getWeightOfSociability()*s+(1-Utils.getWeightOfSociability())*e;
                        }
                    });
                    neighbors=new ArrayList<>();
                    for(int i=0;i<Utils.getMaxNumOfNeighbors()&& i<al.size();i++){
                        neighbors.add(al.get(i));
                    }
                    // Sort and update
                    System.out.println("Sort and Update Neighbours ");
                })
                .match(Message.AddAcquaintances.class, ref -> {
                    Neighbor temp=new Neighbor();
                    temp.setName(ref.ref);
                    temp.setExpertise(new double[]{0.5, 0.5, 0.5, 0.5});
                    temp.setSociability(new double[]{0.5, 0.5, 0.5, 0.5});
                    acquaintances.put(ref.ref,temp);
                    System.out.println("Added Acquaintance" + ref.ref);
                })
                .match(Message.GetNeighboursAndAcquaintances.class, getNeighboursAndAcquaintances -> {
                    Neighbor[] al=new Neighbor[acquaintances.size()];
                    int i=0;
                    for(String s:acquaintances.keySet()){
                        al[i]=acquaintances.get(s);
                        i++;
                    }
                    i=0;
                    Neighbor[] nl=new Neighbor[neighbors.size()];
                    for(Neighbor n:neighbors){
                        nl[i]=n;
                        i++;
                    }
                    getSender().tell(new Message.NeighboursAndAcquaintances(nl,al), getSender());
                })
                .build();
    }
}
