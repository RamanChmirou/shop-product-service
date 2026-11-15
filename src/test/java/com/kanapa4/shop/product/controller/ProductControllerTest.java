package com.kanapa4.shop.product.controller;

import com.kanapa4.shop.product.dto.*;
import com.kanapa4.shop.product.exception.InvalidProductConfigurationException;
import com.kanapa4.shop.product.exception.ProductNotFoundException;
import com.kanapa4.shop.product.model.ProductType;
import com.kanapa4.shop.product.model.VatRate;
import com.kanapa4.shop.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private static PriceDto priceDto() {
        return new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23);
    }

    private static ProductSummaryDto summaryDto() {
        return new ProductSummaryDto(PRODUCT_ID, "Laptop Pro 15", ProductType.COMPUTER, priceDto());
    }

    private static ProductDto productDto() {
        return new ProductDto(PRODUCT_ID, "Laptop Pro 15", "description", ProductType.COMPUTER,
                priceDto(), Map.of("screen", "15.6 inch"), true);
    }

    private static ProductRequest productRequest(String name, BigDecimal amount, String currency) {
        return new ProductRequest(name, "description", ProductType.ELECTRONICS,
                new PriceRequest(amount, currency, VatRate.VAT_23), Map.of(), List.of());
    }

    private static ProductRequest validProductRequest() {
        return productRequest("Bluetooth Speaker", new BigDecimal("199.00"), "PLN");
    }

    @Test
    void getAll_ProductsExist_Return200WithPage() throws Exception {
        when(productService.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summaryDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.content[0].price.amount").value(3999.00))
                .andExpect(jsonPath("$.content[0].price.vatRate").value("VAT_23"));
    }

    @Test
    void getAll_PageParams_PassedToService() throws Exception {
        when(productService.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/products").param("page", "1").param("size", "5").param("sort", "name,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAll(captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("name").getDirection());
    }

    @Test
    void getAll_NoParams_UseDefaultPageSizeAndSortByName() throws Exception {
        when(productService.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/products")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAll(captor.capture());
        assertEquals(20, captor.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, captor.getValue().getSort().getOrderFor("name").getDirection());
    }

    @Test
    void getByType_ValidType_Return200() throws Exception {
        when(productService.getByType(eq(ProductType.COMPUTER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summaryDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/products").param("type", "COMPUTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("COMPUTER"));
    }

    @Test
    void getByType_UnknownType_Return400() throws Exception {
        mockMvc.perform(get("/products").param("type", "FURNITURE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_ProductExists_Return200() throws Exception {
        when(productService.getById(PRODUCT_ID)).thenReturn(productDto());

        mockMvc.perform(get("/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.attributes.screen").value("15.6 inch"));
    }

    @Test
    void getById_ProductNotFound_Return404WithProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getById(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void getById_InvalidUuid_Return400() throws Exception {
        mockMvc.perform(get("/products/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConfiguration_ProductExists_Return200() throws Exception {
        when(productService.getConfiguration(PRODUCT_ID))
                .thenReturn(new ProductConfigurationDto(PRODUCT_ID, "Laptop Pro 15", priceDto(), List.of()));

        mockMvc.perform(get("/products/{id}/configuration", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.optionGroups").isArray());
    }

    @Test
    void getConfiguration_ProductNotFound_Return404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getConfiguration(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/products/{id}/configuration", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void quote_ValidSelection_Return200() throws Exception {
        PriceQuoteRequest request = new PriceQuoteRequest(List.of(
                new SelectedOptionRequest("PROCESSOR", "I7"), new SelectedOptionRequest("RAM", "RAM_32")));
        PriceDto unitPrice = new PriceDto(new BigDecimal("5199.00"), "PLN", VatRate.VAT_23);
        when(productService.quote(eq(PRODUCT_ID), any(PriceQuoteRequest.class)))
                .thenReturn(new PriceQuoteDto(PRODUCT_ID, "Laptop Pro 15", unitPrice, List.of()));

        mockMvc.perform(post("/products/{id}/price-quote", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice.amount").value(5199.00))
                .andExpect(jsonPath("$.unitPrice.currency").value("PLN"));
    }

    @Test
    void quote_InvalidSelection_Return400() throws Exception {
        PriceQuoteRequest request = new PriceQuoteRequest(List.of(new SelectedOptionRequest("PROCESSOR", "I7")));
        when(productService.quote(eq(PRODUCT_ID), any(PriceQuoteRequest.class)))
                .thenThrow(new InvalidProductConfigurationException("Required option group RAM is missing"));

        mockMvc.perform(post("/products/{id}/price-quote", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Required option group RAM is missing"));
    }

    @Test
    void quote_ProductNotFound_Return404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.quote(eq(id), any(PriceQuoteRequest.class))).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(post("/products/{id}/price-quote", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PriceQuoteRequest(List.of()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void quote_MissingSelectedOptions_Return400() throws Exception {
        mockMvc.perform(post("/products/{id}/price-quote", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).quote(any(), any());
    }

    @Test
    void create_ValidRequest_Return201() throws Exception {
        when(productService.create(any(ProductRequest.class))).thenReturn(productDto());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()));
    }

    @Test
    void create_BlankName_Return400() throws Exception {
        ProductRequest request = productRequest("", new BigDecimal("199.00"), "PLN");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).create(any());
    }

    @Test
    void create_NegativePrice_Return400() throws Exception {
        ProductRequest request = productRequest("Speaker", new BigDecimal("-1.00"), "PLN");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).create(any());
    }

    @Test
    void create_InvalidCurrencyCode_Return400() throws Exception {
        ProductRequest request = productRequest("Speaker", new BigDecimal("199.00"), "pln");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).create(any());
    }

    @Test
    void create_MissingPrice_Return400() throws Exception {
        ProductRequest request = new ProductRequest("Speaker", "description", ProductType.ELECTRONICS,
                null, Map.of(), List.of());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).create(any());
    }

    @Test
    void create_ServiceRejectsConfiguration_Return400() throws Exception {
        when(productService.create(any(ProductRequest.class)))
                .thenThrow(new InvalidProductConfigurationException("Electronics cannot have option groups"));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Electronics cannot have option groups"));
    }

    @Test
    void update_ValidRequest_Return200() throws Exception {
        when(productService.update(eq(PRODUCT_ID), any(ProductRequest.class))).thenReturn(productDto());

        mockMvc.perform(put("/products/{id}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro 15"));
    }

    @Test
    void update_ProductNotFound_Return404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.update(eq(id), any(ProductRequest.class))).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_InvalidBody_Return400() throws Exception {
        ProductRequest request = productRequest("", new BigDecimal("199.00"), "PLN");

        mockMvc.perform(put("/products/{id}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).update(any(), any());
    }

    @Test
    void delete_ProductExists_Return204() throws Exception {
        mockMvc.perform(delete("/products/{id}", PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(productService).delete(PRODUCT_ID);
    }

    @Test
    void delete_ProductNotFound_Return404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ProductNotFoundException(id)).when(productService).delete(id);

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    private static BatchPriceQuoteRequest batchRequest(int size) {
        return new BatchPriceQuoteRequest(IntStream.range(0, size)
                .mapToObj(index -> new BatchPriceQuoteItemRequest(PRODUCT_ID, List.of()))
                .toList());
    }

    @Test
    void quoteAll_ValidRequest_Return200WithQuotes() throws Exception {
        PriceDto first = new PriceDto(new BigDecimal("5199.00"), "PLN", VatRate.VAT_23);
        PriceDto second = new PriceDto(new BigDecimal("4799.00"), "PLN", VatRate.VAT_23);
        when(productService.quoteAll(any(BatchPriceQuoteRequest.class)))
                .thenReturn(new BatchPriceQuoteResponse(List.of(
                        new PriceQuoteDto(PRODUCT_ID, "Laptop Pro 15", first, List.of()),
                        new PriceQuoteDto(PRODUCT_ID, "Laptop Pro 15", second, List.of()))));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotes.length()").value(2))
                .andExpect(jsonPath("$.quotes[0].unitPrice.amount").value(5199.00))
                .andExpect(jsonPath("$.quotes[1].unitPrice.amount").value(4799.00));
    }

    @Test
    void quoteAll_MaxSizeRequest_Return200() throws Exception {
        when(productService.quoteAll(any(BatchPriceQuoteRequest.class)))
                .thenReturn(new BatchPriceQuoteResponse(List.of()));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest(50))))
                .andExpect(status().isOk());
    }

    @Test
    void quoteAll_EmptyItems_Return400() throws Exception {
        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest(0))))
                .andExpect(status().isBadRequest());

        verify(productService, never()).quoteAll(any());
    }

    @Test
    void quoteAll_MissingItems_Return400() throws Exception {
        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).quoteAll(any());
    }

    @Test
    void quoteAll_TooManyItems_Return400() throws Exception {
        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest(51))))
                .andExpect(status().isBadRequest());

        verify(productService, never()).quoteAll(any());
    }

    @Test
    void quoteAll_MissingProductId_Return400() throws Exception {
        BatchPriceQuoteRequest request = new BatchPriceQuoteRequest(
                List.of(new BatchPriceQuoteItemRequest(null, List.of())));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).quoteAll(any());
    }

    @Test
    void quoteAll_MissingSelectedOptions_Return400() throws Exception {
        BatchPriceQuoteRequest request = new BatchPriceQuoteRequest(
                List.of(new BatchPriceQuoteItemRequest(PRODUCT_ID, null)));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).quoteAll(any());
    }

    @Test
    void quoteAll_ProductNotFound_Return404() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.quoteAll(any(BatchPriceQuoteRequest.class))).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest(1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void quoteAll_InvalidSelection_Return400() throws Exception {
        when(productService.quoteAll(any(BatchPriceQuoteRequest.class)))
                .thenThrow(new InvalidProductConfigurationException("Required option group RAM is missing"));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Required option group RAM is missing"));
    }
}
