package com.example.demo;

import org.springframework.boot.SpringApplication;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class SpringBootApplication {
	public static final String BLOCKED_PRODUCTS_STORE_NAME = "blocked-products-store";

	public static void main(String[] args) {
		SpringApplication.run(SpringBootApplication.class, args);
	}

}
