package com.kanapa4.shop.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PriceQuoteRequest(
        @NotNull @Valid List<SelectedOptionRequest> selectedOptions) {
}