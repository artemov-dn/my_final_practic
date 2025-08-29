package com.example.demo.serializers;

import com.example.demo.domain.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class ProductDeserializer implements Deserializer<Product> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Product deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            Product result = objectMapper.readValue(data, Product.class);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка десериализации объекта Message", e);
        }
    }
}