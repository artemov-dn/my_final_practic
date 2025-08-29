package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Request {
    @JsonProperty
    private String request_id;
    @JsonProperty
    private String client_id;
    @JsonProperty
    private String action;
    @JsonProperty
    private String description;
    @JsonProperty
    private String created_at;

    public Request() {}

    public Request(String request_id, String client_id, String action, String description, String created_at) {
        this.request_id = request_id;
        this.client_id = client_id;
        this.action = action;
        this.description = description;
        this.created_at = created_at;
    }

    public String getRequest_id() {
        return request_id;
    }

    public String getClient_id() {
        return client_id;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public String getCreated_at() {
        return created_at;
    }
}
