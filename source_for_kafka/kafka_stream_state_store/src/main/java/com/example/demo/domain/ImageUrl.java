package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageUrl {
    @JsonProperty
    private String url;
    @JsonProperty
    private String alt;

    public String getUrl() {
        return url;
    }

    public String getAlt() {
        return alt;
    }
}
