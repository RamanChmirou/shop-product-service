package com.kanapa4.shop.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BatchPriceQuoteItemRequest(
        @NotNull UUID productId,
        @NotNull @Valid List<SelectedOptionRequest> selectedOptions) {
}