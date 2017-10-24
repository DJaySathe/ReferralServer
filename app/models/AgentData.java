package models;

import java.util.ArrayList;
import java.util.Arrays;

public class AgentData {
    String name;
    double[] expertise;
    double[] needs;
    ArrayList<Neighbor> neighbors;
    ArrayList<Neighbor> acquaintances;

    public AgentData(String name, double[] expertise, double[] needs, ArrayList<Neighbor> neighbors, ArrayList<Neighbor> acquaintances) {
        this.name = name;
        this.expertise = expertise;
        this.needs = needs;
        this.neighbors = neighbors;
        this.acquaintances = acquaintances;
    }

    @Override
    public String toString() {
        return "AgentData{" +
                "name='" + name + '\'' +
                ", expertise=" + Arrays.toString(expertise) +
                ", needs=" + Arrays.toString(needs) +
                ", neighbors=" + neighbors +
                ", acquaintances=" + acquaintances +
                '}';
    }

    public ArrayList<Neighbor> getAcquaintances() {
        return acquaintances;
    }

    public void setAcquaintances(ArrayList<Neighbor> acquaintances) {
        this.acquaintances = acquaintances;
    }

    public AgentData(){

    }

    public ArrayList<Neighbor> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<Neighbor> neighbors) {
        this.neighbors = neighbors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double[] getExpertise() {
        return expertise;
    }

    public void setExpertise(double[] expertise) {
        this.expertise = expertise;
    }

    public double[] getNeeds() {
        return needs;
    }

    public void setNeeds(double[] needs) {
        this.needs = needs;
    }


}
