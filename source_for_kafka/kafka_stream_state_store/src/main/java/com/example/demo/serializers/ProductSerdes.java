package com.example.demo.serializers;

import com.example.demo.domain.Product;
import org.apache.kafka.common.serialization.Serdes;

public class ProductSerdes extends Serdes.WrapperSerde<Product> {
    public ProductSerdes() {
        super(new ProductSerializer(), new ProductDeserializer());
    }
}