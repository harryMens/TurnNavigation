package com.google.android.turnnavigation.boundary;

import java.util.List;

public class Geometry {
    List<List<List<List<List<Double>>>>> coordinates;

    public List<List<List<List<List<Double>>>>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<List<List<List<Double>>>>> coordinates) {
        this.coordinates = coordinates;
    }
}
