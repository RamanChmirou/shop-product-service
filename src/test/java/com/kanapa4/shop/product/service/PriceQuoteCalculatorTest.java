package com.kanapa4.shop.product.service;

import com.kanapa4.shop.product.dto.PriceQuoteDto;
import com.kanapa4.shop.product.dto.SelectedOptionDto;
import com.kanapa4.shop.product.dto.SelectedOptionRequest;
import com.kanapa4.shop.product.mapper.ProductMapper;
import com.kanapa4.shop.product.model.Product;
import com.kanapa4.shop.product.model.VatRate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static com.kanapa4.shop.product.testdata.ProductFixtures.LAPTOP_ID;
import static com.kanapa4.shop.product.testdata.ProductFixtures.laptop;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PriceQuoteCalculatorTest {

    @Spy
    private ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    @InjectMocks
    private PriceQuoteCalculator calculator;

    private final Product laptop = laptop();

    private static SelectedOptionRequest option(String groupCode, String valueCode) {
        return new SelectedOptionRequest(groupCode, valueCode);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void calculate_OptionsWithoutSurcharge_ReturnBasePrice() {
        PriceQuoteDto result = calculator.calculate(laptop,
                List.of(option("PROCESSOR", "I5"), option("RAM", "RAM_16")));

        assertAmount("3999.00", result.unitPrice().amount());
    }

    @Test
    void calculate_SingleSelectionsWithSurcharge_ReturnBasePricePlusSurcharges() {
        PriceQuoteDto result = calculator.calculate(laptop,
                List.of(option("PROCESSOR", "I7"), option("RAM", "RAM_32")));

        assertAmount("5199.00", result.unitPrice().amount());
    }

    @Test
    void calculate_MultiSelectionAccessories_SumsAllSurcharges() {
        PriceQuoteDto result = calculator.calculate(laptop, List.of(
                option("PROCESSOR", "I7"), option("RAM", "RAM_32"),
                option("ACCESSORIES", "CASE"), option("ACCESSORIES", "CHARGER")));

        // 3999 + 800 + 400 + 49 + 99
        assertAmount("5347.00", result.unitPrice().amount());
    }

    @Test
    void calculate_AnyOptions_KeepsCurrencyAndVatRateOfProduct() {
        PriceQuoteDto result = calculator.calculate(laptop,
                List.of(option("PROCESSOR", "I7"), option("RAM", "RAM_32")));

        assertEquals("PLN", result.unitPrice().currency());
        assertEquals(VatRate.VAT_23, result.unitPrice().vatRate());
    }

    @Test
    void calculate_AnyOptions_ReturnProductIdAndName() {
        PriceQuoteDto result = calculator.calculate(laptop,
                List.of(option("PROCESSOR", "I5"), option("RAM", "RAM_16")));

        assertEquals(LAPTOP_ID, result.productId());
        assertEquals("Laptop Pro 15", result.productName());
    }

    @Test
    void calculate_SelectedOptions_ReturnLabelsAndPriceDeltas() {
        PriceQuoteDto result = calculator.calculate(laptop,
                List.of(option("PROCESSOR", "I7"), option("RAM", "RAM_32")));

        assertEquals(2, result.selectedOptions().size());
        SelectedOptionDto processor = result.selectedOptions().stream()
                .filter(selected -> selected.valueCode().equals("I7"))
                .findFirst()
                .orElseThrow();
        assertEquals("PROCESSOR", processor.groupCode());
        assertEquals("Intel Core i7", processor.valueLabel());
        assertAmount("800.00", processor.priceDelta());
    }

    @Test
    void calculate_EmptySelection_ReturnBasePriceAndNoOptions() {
        PriceQuoteDto result = calculator.calculate(laptop, List.of());

        assertAmount("3999.00", result.unitPrice().amount());
        assertTrue(result.selectedOptions().isEmpty());
    }
}