package com.kanapa4.shop.product.service;

import com.kanapa4.shop.product.dto.*;
import com.kanapa4.shop.product.exception.InvalidProductConfigurationException;
import com.kanapa4.shop.product.exception.ProductNotFoundException;
import com.kanapa4.shop.product.mapper.ProductMapper;
import com.kanapa4.shop.product.model.Product;
import com.kanapa4.shop.product.model.ProductType;
import com.kanapa4.shop.product.model.VatRate;
import com.kanapa4.shop.product.repository.ProductRepository;
import com.kanapa4.shop.product.validator.ProductConfigurationValidator;
import com.kanapa4.shop.product.validator.ProductRequestValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static com.kanapa4.shop.product.testdata.ProductFixtures.LAPTOP_ID;
import static com.kanapa4.shop.product.testdata.ProductFixtures.laptop;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductRequestValidator productRequestValidator;

    @Mock
    private ProductConfigurationValidator productConfigurationValidator;

    @Mock
    private PriceQuoteCalculator priceQuoteCalculator;

    @InjectMocks
    private ProductService productService;

    private final Pageable pageable = PageRequest.of(0, 10);

    private static PriceDto priceDto() {
        return new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23);
    }

    private static ProductSummaryDto summaryDto() {
        return new ProductSummaryDto(LAPTOP_ID, "Laptop Pro 15", ProductType.COMPUTER, priceDto());
    }

    private static ProductDto productDto() {
        return new ProductDto(LAPTOP_ID, "Laptop Pro 15", "description", ProductType.COMPUTER,
                priceDto(), Map.of(), true);
    }

    private static ProductRequest productRequest() {
        return new ProductRequest("Laptop Pro 15", "description", ProductType.COMPUTER,
                new PriceRequest(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                Map.of(), List.of());
    }

    @Test
    void getAll_ProductsExist_ReturnMappedPage() {
        Product product = laptop();
        ProductSummaryDto summary = summaryDto();
        when(productRepository.findAllByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(productMapper.toSummary(product)).thenReturn(summary);

        Page<ProductSummaryDto> result = productService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().getFirst());
        verify(productRepository).findAllByActiveTrue(pageable);
    }

    @Test
    void getByType_ProductsExist_ReturnMappedPage() {
        Product product = laptop();
        ProductSummaryDto summary = summaryDto();
        when(productRepository.findAllByTypeAndActiveTrue(ProductType.COMPUTER, pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(productMapper.toSummary(product)).thenReturn(summary);

        Page<ProductSummaryDto> result = productService.getByType(ProductType.COMPUTER, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().getFirst());
        verify(productRepository).findAllByTypeAndActiveTrue(ProductType.COMPUTER, pageable);
    }

    @Test
    void getById_ProductExists_ReturnDto() {
        Product product = laptop();
        ProductDto dto = productDto();
        when(productRepository.findById(LAPTOP_ID)).thenReturn(Optional.of(product));
        when(productMapper.toDto(product)).thenReturn(dto);

        ProductDto result = productService.getById(LAPTOP_ID);

        assertEquals(dto, result);
    }

    @Test
    void getById_ProductNotFound_ThrowsException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getById(id));
    }

    @Test
    void getConfiguration_ProductExists_ReturnConfiguration() {
        Product product = laptop();
        ProductConfigurationDto configuration =
                new ProductConfigurationDto(LAPTOP_ID, "Laptop Pro 15", priceDto(), List.of());
        when(productRepository.findById(LAPTOP_ID)).thenReturn(Optional.of(product));
        when(productMapper.toConfiguration(product)).thenReturn(configuration);

        ProductConfigurationDto result = productService.getConfiguration(LAPTOP_ID);

        assertEquals(configuration, result);
    }

    @Test
    void getConfiguration_ProductNotFound_ThrowsException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getConfiguration(id));
    }

    @Test
    void quote_ValidSelection_ValidatesThenCalculates() {
        Product product = laptop();
        List<SelectedOptionRequest> selected = List.of(
                new SelectedOptionRequest("PROCESSOR", "I7"), new SelectedOptionRequest("RAM", "RAM_32"));
        PriceQuoteRequest request = new PriceQuoteRequest(selected);
        PriceQuoteDto quote = new PriceQuoteDto(LAPTOP_ID, "Laptop Pro 15", priceDto(), List.of());
        when(productRepository.findById(LAPTOP_ID)).thenReturn(Optional.of(product));
        when(priceQuoteCalculator.calculate(product, selected)).thenReturn(quote);

        PriceQuoteDto result = productService.quote(LAPTOP_ID, request);

        assertEquals(quote, result);
        InOrder inOrder = inOrder(productConfigurationValidator, priceQuoteCalculator);
        inOrder.verify(productConfigurationValidator).validate(product, selected);
        inOrder.verify(priceQuoteCalculator).calculate(product, selected);
    }

    @Test
    void quote_InvalidSelection_ThrowsExceptionAndDoesNotCalculate() {
        Product product = laptop();
        List<SelectedOptionRequest> selected = List.of(new SelectedOptionRequest("PROCESSOR", "I7"));
        when(productRepository.findById(LAPTOP_ID)).thenReturn(Optional.of(product));
        doThrow(new InvalidProductConfigurationException("Required group missing"))
                .when(productConfigurationValidator).validate(product, selected);

        assertThrows(InvalidProductConfigurationException.class,
                () -> productService.quote(LAPTOP_ID, new PriceQuoteRequest(selected)));
        verifyNoInteractions(priceQuoteCalculator);
    }

    @Test
    void quote_ProductNotFound_ThrowsExceptionAndSkipsValidation() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.quote(id, new PriceQuoteRequest(List.of())));
        verifyNoInteractions(productConfigurationValidator, priceQuoteCalculator);
    }

    @Test
    void create_ValidRequest_ValidatesSavesAndReturnDto() {
        ProductRequest request = productRequest();
        Product product = laptop();
        ProductDto dto = productDto();
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDto(product)).thenReturn(dto);

        ProductDto result = productService.create(request);

        assertEquals(dto, result);
        verify(productRequestValidator).validate(request);
        verify(productRepository).save(product);
    }

    @Test
    void create_InvalidRequest_ThrowsExceptionAndDoesNotSave() {
        ProductRequest request = productRequest();
        doThrow(new InvalidProductConfigurationException("Invalid"))
                .when(productRequestValidator).validate(request);

        assertThrows(InvalidProductConfigurationException.class, () -> productService.create(request));
        verifyNoInteractions(productRepository, productMapper);
    }

    @Test
    void update_ProductExists_ClearsGroupsFlushesThenUpdates() {
        Product product = laptop();
        ProductRequest request = productRequest();
        ProductDto dto = productDto();
        when(productRepository.findById(LAPTOP_ID)).thenReturn(Optional.of(product));
        when(productMapper.toDto(product)).thenReturn(dto);

        ProductDto result = productService.update(LAPTOP_ID, request);

        assertEquals(dto, result);
        assertTrue(product.getOptionGroups().isEmpty());
        verify(productRequestValidator).validate(request);
        InOrder inOrder = inOrder(productRepository, productMapper);
        inOrder.verify(productRepository).flush();
        inOrder.verify(productMapper).update(product, request);
    }

    @Test
    void update_ProductNotFound_ThrowsExceptionAndDoesNotUpdate() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.update(id, productRequest()));
        verify(productMapper, never()).update(any(), any());
    }

    @Test
    void delete_ProductExists_DeletesProduct() {
        Product product = laptop();
        when(productRepository.findById(LAPTOP_ID)).thenReturn(Optional.of(product));

        productService.delete(LAPTOP_ID);

        verify(productRepository).delete(product);
    }

    @Test
    void delete_ProductNotFound_ThrowsExceptionAndDeletesNothing() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.delete(id));
        verify(productRepository, never()).delete(any(Product.class));
    }

    private static BatchPriceQuoteRequest batch(BatchPriceQuoteItemRequest... items) {
        return new BatchPriceQuoteRequest(List.of(items));
    }

    @Test
    void quoteAll_SameProductTwoConfigurations_ReturnQuotesInRequestOrderWithSingleLookup() {
        Product product = laptop();
        List<SelectedOptionRequest> ram16 = List.of(new SelectedOptionRequest("RAM", "RAM_16"));
        List<SelectedOptionRequest> ram32 = List.of(new SelectedOptionRequest("RAM", "RAM_32"));
        PriceQuoteDto quote16 = new PriceQuoteDto(LAPTOP_ID, "Laptop Pro 15", priceDto(), List.of());
        PriceQuoteDto quote32 = new PriceQuoteDto(LAPTOP_ID, "Laptop Pro 15", priceDto(), List.of());
        when(productRepository.findAllById(Set.of(LAPTOP_ID))).thenReturn(List.of(product));
        when(priceQuoteCalculator.calculate(product, ram16)).thenReturn(quote16);
        when(priceQuoteCalculator.calculate(product, ram32)).thenReturn(quote32);

        BatchPriceQuoteResponse result = productService.quoteAll(batch(
                new BatchPriceQuoteItemRequest(LAPTOP_ID, ram16),
                new BatchPriceQuoteItemRequest(LAPTOP_ID, ram32)));

        assertEquals(2, result.quotes().size());
        assertSame(quote16, result.quotes().get(0));
        assertSame(quote32, result.quotes().get(1));
        verify(productRepository).findAllById(Set.of(LAPTOP_ID));
        verify(productConfigurationValidator).validate(product, ram16);
        verify(productConfigurationValidator).validate(product, ram32);
    }

    @Test
    void quoteAll_OneProductMissing_ThrowsExceptionAndCalculatesNothing() {
        UUID missingId = UUID.randomUUID();
        when(productRepository.findAllById(Set.of(LAPTOP_ID, missingId))).thenReturn(List.of(laptop()));

        assertThrows(ProductNotFoundException.class, () -> productService.quoteAll(batch(
                new BatchPriceQuoteItemRequest(LAPTOP_ID, List.of()),
                new BatchPriceQuoteItemRequest(missingId, List.of()))));
        verifyNoInteractions(productConfigurationValidator, priceQuoteCalculator);
    }

    @Test
    void quoteAll_InvalidSelection_ThrowsException() {
        Product product = laptop();
        List<SelectedOptionRequest> selected = List.of(new SelectedOptionRequest("PROCESSOR", "I7"));
        when(productRepository.findAllById(Set.of(LAPTOP_ID))).thenReturn(List.of(product));
        doThrow(new InvalidProductConfigurationException("Required group missing"))
                .when(productConfigurationValidator).validate(product, selected);

        assertThrows(InvalidProductConfigurationException.class, () -> productService.quoteAll(
                batch(new BatchPriceQuoteItemRequest(LAPTOP_ID, selected))));
        verifyNoInteractions(priceQuoteCalculator);
    }
}