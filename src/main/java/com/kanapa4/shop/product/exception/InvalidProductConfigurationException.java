package com.kanapa4.shop.product.exception;

import org.springframework.http.HttpStatus;

public class InvalidProductConfigurationException extends ProductServiceException {

    public InvalidProductConfigurationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}