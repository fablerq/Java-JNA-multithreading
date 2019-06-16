package com.aopi.models;

import java.util.ArrayList;

public class dataRequest {
    public ArrayList<String> abscissas;
    public ArrayList<String> ordinates;
    public String type;

    public dataRequest(ArrayList<String> abscissas,
                       ArrayList<String> ordinates,
                       String type) {
        this.abscissas = abscissas;
        this.ordinates = ordinates;
        this.type = type;
    }


    @Override
    public String toString() {
        return "dataRequest{" +
                "abscissas=" + abscissas +
                ", ordinates=" + ordinates +
                ", type=" + type +
                '}';
    }
}
