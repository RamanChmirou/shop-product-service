package com.kanapa4.shop.product.dto;

import com.kanapa4.shop.product.model.VatRate;

import java.math.BigDecimal;

public record PriceDto(
        BigDecimal amount,
        String currency,
        VatRate vatRate) {
}