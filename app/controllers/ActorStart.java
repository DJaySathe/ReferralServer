package controllers;

import actors.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import models.AgentData;
import play.mvc.Controller;

import java.util.HashMap;
import java.util.TreeMap;

public class ActorStart extends Controller{

    public static TreeMap<String, ActorRef> ActorMap;
    public static ActorSystem actorSystem;
    public static void initialiseSystem(AgentData[] agentList) {
        actorSystem=ActorSystem.create("ReferralActorSystem");
        ActorMap=new TreeMap<>();
        for(int i=0;i<agentList.length;i++){
            ActorMap.put(agentList[i].getName(),actorSystem.actorOf(ReferralAgent.props(agentList[i]),agentList[i].getName()));
        }

    }

}