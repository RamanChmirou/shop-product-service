package com.kanapa4.shop.product.service;

import com.kanapa4.shop.product.dto.PriceQuoteDto;
import com.kanapa4.shop.product.dto.SelectedOptionDto;
import com.kanapa4.shop.product.dto.SelectedOptionRequest;
import com.kanapa4.shop.product.mapper.ProductMapper;
import com.kanapa4.shop.product.model.Price;
import com.kanapa4.shop.product.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceQuoteCalculator {

    private final ProductMapper productMapper;

    public PriceQuoteDto calculate(Product product, List<SelectedOptionRequest> selectedOptions) {
        log.debug("Calculating quote for product {} with {} selected options", product.getId(), selectedOptions.size());
        List<SelectedOptionDto> options = resolveOptions(product, selectedOptions);
        Price unitPrice = addSurcharges(product.getBasePrice(), options);

        return new PriceQuoteDto(
                product.getId(),
                product.getName(),
                productMapper.toPriceDto(unitPrice),
                options);
    }

    private List<SelectedOptionDto> resolveOptions(Product product, List<SelectedOptionRequest> selectedOptions) {
        Set<SelectedOptionRequest> selected = new HashSet<>(selectedOptions);

        return product.getOptionGroups().stream()
                .flatMap(group -> group.getValues().stream()
                        .filter(value -> selected.contains(
                                new SelectedOptionRequest(group.getCode(), value.getCode())))
                        .map(value -> new SelectedOptionDto(
                                group.getCode(),
                                value.getCode(),
                                value.getLabel(),
                                value.getPriceDelta())))
                .toList();
    }

    private Price addSurcharges(Price basePrice, List<SelectedOptionDto> options) {
        BigDecimal surcharges = options.stream()
                .map(SelectedOptionDto::priceDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return basePrice.plus(new Price(surcharges, basePrice.currency(), basePrice.vatRate()));
    }
}