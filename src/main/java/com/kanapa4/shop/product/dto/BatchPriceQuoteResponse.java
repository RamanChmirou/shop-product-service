package com.kanapa4.shop.product.dto;

import java.util.List;

public record BatchPriceQuoteResponse(List<PriceQuoteDto> quotes) {
}