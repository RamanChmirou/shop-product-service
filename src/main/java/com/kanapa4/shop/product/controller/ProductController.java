package com.kanapa4.shop.product.controller;

import com.kanapa4.shop.product.dto.PriceQuoteDto;
import com.kanapa4.shop.product.dto.PriceQuoteRequest;
import com.kanapa4.shop.product.dto.ProductConfigurationDto;
import com.kanapa4.shop.product.dto.ProductDto;
import com.kanapa4.shop.product.dto.ProductRequest;
import com.kanapa4.shop.product.dto.ProductSummaryDto;
import com.kanapa4.shop.product.model.ProductType;
import com.kanapa4.shop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@Tag(name = "Product Controller", description = "Operations related to managing products and price quotes")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get paginated list of products",
            description = "Retrieves active products with pagination and sorting.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of products"),
            @ApiResponse(responseCode = "400", description = "Unknown sort property")
    })
    @GetMapping
    public Page<ProductSummaryDto> getAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        log.info("GET /products pageable={}", pageable);
        return productService.getAll(pageable);
    }

    @Operation(summary = "Get paginated list of products by type",
            description = "Retrieves active products of the given type (COMPUTER, SMARTPHONE, ELECTRONICS) with pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of products"),
            @ApiResponse(responseCode = "400", description = "Unknown product type or sort property")
    })
    @GetMapping(params = "type")
    public Page<ProductSummaryDto> getByType(@RequestParam ProductType type,
                                             @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        log.info("GET /products type={} pageable={}", type, pageable);
        return productService.getByType(type, pageable);
    }

    @Operation(summary = "Get product by ID",
            description = "Retrieves a single product by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found and returned"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable UUID id) {
        log.info("GET /products/{}", id);
        return productService.getById(id);
    }

    @Operation(summary = "Get product configuration",
            description = "Retrieves option groups and values the customer can choose for the product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration returned"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/configuration")
    public ProductConfigurationDto getConfiguration(@PathVariable UUID id) {
        log.info("GET /products/{}/configuration", id);
        return productService.getConfiguration(id);
    }

    @Operation(summary = "Calculate price quote",
            description = "Calculates the unit price of the product for the selected options.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Price quote calculated"),
            @ApiResponse(responseCode = "400", description = "Invalid option selection"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/{id}/price-quote")
    public PriceQuoteDto quote(@PathVariable UUID id, @Valid @RequestBody PriceQuoteRequest request) {
        log.info("POST /products/{}/price-quote, {} selected options", id, request.selectedOptions().size());
        return productService.quote(id, request);
    }

    @Operation(summary = "Create a new product",
            description = "Creates a new product record with optional configuration option groups.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        log.info("POST /products name={} type={}", request.name(), request.type());
        return productService.create(request);
    }

    @Operation(summary = "Update a product",
            description = "Replaces the product data and its option groups.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ProductDto update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        log.info("PUT /products/{}", id);
        return productService.update(id, request);
    }

    @Operation(summary = "Delete a product",
            description = "Deletes the product together with its option groups.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        log.info("DELETE /products/{}", id);
        productService.delete(id);
    }
}