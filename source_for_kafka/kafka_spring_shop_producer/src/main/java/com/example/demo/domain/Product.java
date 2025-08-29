package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Product {
    @JsonProperty
    private String product_id;
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private Price price;
    @JsonProperty
    private String category;
    @JsonProperty
    private String brand;
    @JsonProperty
    private Stock stock;
    @JsonProperty
    private String sku;
    @JsonProperty
    private List<String> tags;
    @JsonProperty
    private List<ImageUrl> images;
    @JsonProperty
    private Specifications specifications;
    @JsonProperty
    private String created_at;
    @JsonProperty
    private String updated_at;
    @JsonProperty
    private String index;
    @JsonProperty
    private String store_id;

    public Product() {}

    // Конструктор
//    public Product(int id, String fio) {
//        this.id = id;
//        this.fio = fio;
//    }

    // Геттеры и сеттеры

    public String getProduct_id() {
        return product_id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Price getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public Stock getStock() {
        return stock;
    }

    public String getSku() {
        return sku;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<ImageUrl> getImages() {
        return images;
    }

    public Specifications getSpecifications() {
        return specifications;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public String getIndex() {
        return index;
    }

    public String getStore_id() {
        return store_id;
    }

//    @Override
//    public String toString() {
//        return "{ \"id\": " + this.id + ", \"fio\":\"" + this.fio + "\" }";
//    }
}
