package com.kanapa4.shop.product.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ProductNotFoundException extends ProductServiceException {

    public ProductNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Product not found: " + id);
    }
}