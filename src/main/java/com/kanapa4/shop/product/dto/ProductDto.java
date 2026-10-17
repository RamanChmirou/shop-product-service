package com.kanapa4.shop.product.dto;

import com.kanapa4.shop.product.model.ProductType;

import java.util.Map;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String description,
        ProductType type,
        PriceDto price,
        Map<String, String> attributes,
        boolean active) {
}