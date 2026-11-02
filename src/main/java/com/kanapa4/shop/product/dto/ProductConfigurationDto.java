package com.kanapa4.shop.product.dto;

import java.util.List;
import java.util.UUID;

public record ProductConfigurationDto(
        UUID productId,
        String productName,
        PriceDto basePrice,
        List<OptionGroupDto> optionGroups) {
}