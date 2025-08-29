package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stock {
    @JsonProperty
    private Integer available;
    @JsonProperty
    private Integer reserved;

    public Integer getAvailable() {
        return available;
    }

    public Integer getReserved() {
        return reserved;
    }
}
