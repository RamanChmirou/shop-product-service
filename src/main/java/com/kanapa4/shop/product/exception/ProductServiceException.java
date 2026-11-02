package com.kanapa4.shop.product.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ProductServiceException extends RuntimeException {

    private final HttpStatus status;

    public ProductServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}