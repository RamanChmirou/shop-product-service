package com.kanapa4.shop.product.dto;

import jakarta.validation.constraints.NotBlank;

public record SelectedOptionRequest(
        @NotBlank String groupCode,
        @NotBlank String valueCode) {
}