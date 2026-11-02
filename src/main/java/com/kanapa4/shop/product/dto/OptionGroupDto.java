package com.kanapa4.shop.product.dto;

import com.kanapa4.shop.product.model.SelectionType;

import java.util.List;

public record OptionGroupDto(
        String code,
        String name,
        SelectionType selectionType,
        boolean required,
        List<OptionValueDto> values) {
}