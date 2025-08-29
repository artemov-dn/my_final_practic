package com.example.demo.service;

import com.example.demo.domain.Request;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class RequestsSender {
    public static final Logger LOG = LoggerFactory.getLogger(RequestsSender.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${requests-location}")
    String requestsLocation;

    @Autowired
    private com.example.demo.service.KafkaProducer kafkaProducer;

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() throws Exception {
        // вычитываем список запросов для отправки в кафку
        List<Request> requests = new ArrayList<>();
        try {
            requests = objectMapper.readValue(new File(requestsLocation), new TypeReference<>() {
            });
        } catch (Exception e) {
        }
        if (requests.isEmpty()) {
            return;
        }
        for (Request request : requests) {
            kafkaProducer.sendMessages(request);
        }
    }

}
