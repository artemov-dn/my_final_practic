package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Specifications {
    @JsonProperty
    private String weight;
    @JsonProperty
    private String dimensions;
    @JsonProperty
    private String battery_life;
    @JsonProperty
    private String water_resistance;

    public String getWeight() {
        return weight;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getBattery_life() {
        return battery_life;
    }

    public String getWater_resistance() {
        return water_resistance;
    }
}
