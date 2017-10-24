package models;

import java.util.Arrays;

public class Neighbor {
    String name;
    double[] expertise;
    double[] sociability;

    public Neighbor(String name, double[] expertise, double[] sociability) {
        this.name = name;
        this.expertise = expertise;
        this.sociability = sociability;
    }

    public Neighbor(){

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

    public double[] getSociability() {
        return sociability;
    }

    public void setSociability(double[] sociability) {
        this.sociability = sociability;
    }
}
