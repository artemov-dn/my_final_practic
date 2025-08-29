package com.example.demo.controller;

import com.example.demo.service.KafkaProducer;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class BlockedProductsController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private KafkaProducer kafkaProducer;

    /**
     * Заблокировать товар
     */
    @RequestMapping(value = "/block/product/{id}", method = RequestMethod.GET)
    public String blockProduct(
            @Parameter(description = "Идентификатор товар", required = true) @PathVariable("id") String id) {
        logger.info("BLOCK products {}", id);

        kafkaProducer.sendMessages(id, "+Товар" + id);
        return id + " blocked";
    }

    /**
     * Удалить товар из заблокированных
     */
    @RequestMapping(value = "/unblock/product/{id}", method = RequestMethod.GET)
    public String unblockProduct(
            @Parameter(description = "Идентификатор товар", required = true) @PathVariable("id") String id) {
        logger.info("UNBLOCK products {}", id);

        kafkaProducer.sendMessages(id, "-Товар" + id);
        return id + " unblocked";
    }

}
