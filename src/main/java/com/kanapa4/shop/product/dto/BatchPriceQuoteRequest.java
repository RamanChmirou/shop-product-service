package com.kanapa4.shop.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchPriceQuoteRequest(
        @NotEmpty @Size(max = 50) @Valid List<BatchPriceQuoteItemRequest> items) {
}