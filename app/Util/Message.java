package Util;

import models.Neighbor;

public class Message {

    static public class Query{
        public String sender;
        public String Reciever;
        public double[] query;
        public int depth;

        public Query(String sender, String reciever, double[] query,int depth) {
            this.sender = sender;
            Reciever = reciever;
            this.query = query;
            this.depth=depth;
        }
    }
    static public class Referral{
        public String sender;
        public String Reciever;
        public String ReferredActor;
        public double[] query;

        public Referral(String sender, String reciever,String ReferredActor,double[] query) {
            this.sender = sender;
            this.Reciever = reciever;
            this.ReferredActor = ReferredActor;
            this.query=query;
        }
    }
    static public class Response{
        public boolean gotAnswer;
        public String sender;
        public String Reciever;
        public double[] ans;

        public Response(String sender, String reciever, double[] ans,boolean gotAnswer) {
            this.sender = sender;
            this.Reciever = reciever;
            this.ans = ans;
            this.gotAnswer=gotAnswer;
        }
    }

    static public class GenerateAndExecute{

    }
    static public class GetNeeds{

    }
    static public class Needs{
        public double[] needs;

        public Needs(double[] needs) {
            this.needs = needs;
        }
    }
    static public class SetNeeds{
        public double[] needs;

        public SetNeeds(double[] needs) {
            this.needs = needs;
        }
    }

    static public class AddAcquaintances{
        public  String ref;

        public AddAcquaintances(String ref) {
            this.ref = ref;
        }
    }
    static public class UpdateExpertise{
        public  String ref;
        public double[] query;
        public double[] ans;

        public UpdateExpertise(String ref, double[] query, double[] ans) {
            this.ref = ref;
            this.query = query;
            this.ans = ans;
        }
    }
    static public class UpdateSociability{
        public  String ref;
        public double[] query;
        public double[] ans;
        public int dist;

        public UpdateSociability(String ref, double[] query, double[] ans,int dist) {
            this.ref = ref;
            this.query = query;
            this.ans = ans;
            this.dist=dist;
        }
    }
    static public class UpdateNeighbours{

    }
    static public class GetNeighboursAndAcquaintances{

    }
    static public class NeighboursAndAcquaintances{
        public Neighbor[] neighbors;
        public Neighbor[] acquaintances;

        public NeighboursAndAcquaintances(Neighbor[] neighbors, Neighbor[] acquaintances) {
            this.neighbors = neighbors;
            this.acquaintances = acquaintances;
        }
    }
}
