package com.kanapa4.shop.product.dto;

import java.math.BigDecimal;

public record OptionValueDto(
        String code,
        String label,
        BigDecimal priceDelta) {
}