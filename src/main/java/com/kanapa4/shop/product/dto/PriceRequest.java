package com.kanapa4.shop.product.dto;

import com.kanapa4.shop.product.model.VatRate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PriceRequest(
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull VatRate vatRate) {
}