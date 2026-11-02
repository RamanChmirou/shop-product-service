package com.kanapa4.shop.product.service;

import com.kanapa4.shop.product.dto.*;
import com.kanapa4.shop.product.exception.ProductNotFoundException;
import com.kanapa4.shop.product.mapper.ProductMapper;
import com.kanapa4.shop.product.model.Product;
import com.kanapa4.shop.product.model.ProductType;
import com.kanapa4.shop.product.repository.ProductRepository;
import com.kanapa4.shop.product.validator.ProductConfigurationValidator;
import com.kanapa4.shop.product.validator.ProductRequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductRequestValidator productRequestValidator;
    private final ProductConfigurationValidator productConfigurationValidator;
    private final PriceQuoteCalculator priceQuoteCalculator;

    public Page<ProductSummaryDto> getAll(Pageable pageable) {
        log.debug("Fetching active products, pageable={}", pageable);
        return productRepository.findAllByActiveTrue(pageable).map(productMapper::toSummary);
    }

    public Page<ProductSummaryDto> getByType(ProductType type, Pageable pageable) {
        log.debug("Fetching active products of type {}, pageable={}", type, pageable);
        return productRepository.findAllByTypeAndActiveTrue(type, pageable).map(productMapper::toSummary);
    }

    public ProductDto getById(UUID id) {
        log.debug("Fetching product {}", id);
        return productMapper.toDto(findOrThrow(id));
    }

    public ProductConfigurationDto getConfiguration(UUID id) {
        log.debug("Fetching configuration of product {}", id);
        return productMapper.toConfiguration(findOrThrow(id));
    }

    public PriceQuoteDto quote(UUID id, PriceQuoteRequest request) {
        log.debug("Calculating price quote for product {}", id);
        return calculateQuote(findOrThrow(id), request.selectedOptions());
    }

    public BatchPriceQuoteResponse quoteAll(BatchPriceQuoteRequest request) {
        log.debug("Calculating {} price quotes", request.items().size());
        Map<UUID, Product> products = loadProducts(request.items());
        List<PriceQuoteDto> quotes = request.items().stream()
                .map(item -> calculateQuote(products.get(item.productId()), item.selectedOptions()))
                .toList();
        return new BatchPriceQuoteResponse(quotes);
    }

    @Transactional
    public ProductDto create(ProductRequest request) {
        log.info("Creating product '{}' of type {}", request.name(), request.type());
        productRequestValidator.validate(request);
        Product saved = productRepository.save(productMapper.toEntity(request));
        log.info("Product created with id {}", saved.getId());
        return productMapper.toDto(saved);
    }

    @Transactional
    public ProductDto update(UUID id, ProductRequest request) {
        log.info("Updating product {}", id);
        productRequestValidator.validate(request);
        Product product = findOrThrow(id);
        product.getOptionGroups().clear();
        productRepository.flush();
        productMapper.update(product, request);
        log.info("Product {} updated", id);
        return productMapper.toDto(product);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting product {}", id);
        productRepository.delete(findOrThrow(id));
        log.info("Product {} deleted", id);
    }

    private Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private PriceQuoteDto calculateQuote(Product product, List<SelectedOptionRequest> selectedOptions) {
        productConfigurationValidator.validate(product, selectedOptions);
        return priceQuoteCalculator.calculate(product, selectedOptions);
    }

    private Map<UUID, Product> loadProducts(List<BatchPriceQuoteItemRequest> items) {
        Set<UUID> ids = items.stream()
                .map(BatchPriceQuoteItemRequest::productId)
                .collect(Collectors.toSet());
        Map<UUID, Product> found = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        ids.stream()
                .filter(id -> !found.containsKey(id))
                .findFirst()
                .ifPresent(missing -> {
                    throw new ProductNotFoundException(missing);
                });
        return found;
    }
}