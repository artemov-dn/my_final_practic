package com.example.demo.controller;

import com.example.demo.domain.Request;
import com.example.demo.service.KafkaProducer;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static java.time.LocalTime.now;

@RestController
@RequestMapping("/rest")
public class ClientRequestsController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private KafkaProducer kafkaProducer;

    @RequestMapping(value = "/get/recommendation/{client_id}", method = RequestMethod.GET)
    public String getRecommendation(
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable("client_id") String clientId) {
        logger.info("GET recommendation for client {}", clientId);

        kafkaProducer.sendMessages(new Request(UUID.randomUUID().toString(), clientId, "GET", "recommendation", now().toString()));
        return "Request recommendation for client " + clientId;
    }

    @RequestMapping(value = "/search/product/{name}/{client_id}", method = RequestMethod.GET)
    public String unblockProduct(
            @Parameter(description = "Идентификатор товар", required = true) @PathVariable("name") String productName,
            @Parameter(description = "Идентификатор клиента", required = true) @PathVariable("client_id") String clientId) {
        logger.info("SEARCH product {}", productName);

        kafkaProducer.sendMessages(new Request(UUID.randomUUID().toString(), clientId, "SEARCH", productName, now().toString()));
        return "Request search product " + productName;
    }

}
