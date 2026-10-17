package com.kanapa4.shop.product.dto;

import com.kanapa4.shop.product.model.ProductType;

import java.util.UUID;

public record ProductSummaryDto(
        UUID id,
        String name,
        ProductType type,
        PriceDto price) {
}