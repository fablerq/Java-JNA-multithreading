package com.aopi.models;

import java.io.Serializable;

public class Coordinate implements Serializable {
    public Double abscissa;
    public Double ordinate;

    public Coordinate() {
    }

    public Coordinate(Double abscissa, Double ordinate) {
        this.abscissa = abscissa;
        this.ordinate = ordinate;
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                "abscissa=" + abscissa +
                ", ordinate=" + ordinate +
                '}';
    }

}
