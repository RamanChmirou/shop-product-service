package com.kanapa4.shop.product.validator;

import com.kanapa4.shop.product.dto.OptionGroupRequest;
import com.kanapa4.shop.product.dto.OptionValueRequest;
import com.kanapa4.shop.product.dto.PriceRequest;
import com.kanapa4.shop.product.dto.ProductRequest;
import com.kanapa4.shop.product.exception.InvalidProductConfigurationException;
import com.kanapa4.shop.product.model.ProductType;
import com.kanapa4.shop.product.model.SelectionType;
import com.kanapa4.shop.product.model.VatRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductRequestValidatorTest {

    private final ProductRequestValidator validator = new ProductRequestValidator();

    private static ProductRequest request(ProductType type, List<OptionGroupRequest> groups) {
        return new ProductRequest("Test product", "description", type,
                new PriceRequest(new BigDecimal("100.00"), "PLN", VatRate.VAT_23),
                Map.of(), groups);
    }

    private static OptionGroupRequest group(String code, OptionValueRequest... values) {
        return new OptionGroupRequest(code, code, SelectionType.SINGLE, true, List.of(values));
    }

    private static OptionValueRequest value(String code) {
        return new OptionValueRequest(code, code, BigDecimal.ZERO);
    }

    @Test
    void validate_ComputerWithOptionGroups_DoesNotThrow() {
        ProductRequest request = request(ProductType.COMPUTER,
                List.of(group("RAM", value("RAM_16"), value("RAM_32"))));

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void validate_ElectronicsWithoutOptionGroups_DoesNotThrow() {
        ProductRequest request = request(ProductType.ELECTRONICS, List.of());

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void validate_ElectronicsWithOptionGroups_ThrowsException() {
        ProductRequest request = request(ProductType.ELECTRONICS,
                List.of(group("COLOR", value("BLACK"))));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(request));
    }

    @Test
    void validate_DuplicateGroupCodes_ThrowsException() {
        ProductRequest request = request(ProductType.COMPUTER, List.of(
                group("RAM", value("RAM_16")),
                group("RAM", value("RAM_32"))));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(request));
    }

    @Test
    void validate_DuplicateValueCodesInGroup_ThrowsException() {
        ProductRequest request = request(ProductType.COMPUTER,
                List.of(group("RAM", value("RAM_16"), value("RAM_16"))));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(request));
    }

    @Test
    void validate_SameValueCodeInDifferentGroups_DoesNotThrow() {
        ProductRequest request = request(ProductType.COMPUTER, List.of(
                group("RAM", value("STANDARD")),
                group("STORAGE", value("STANDARD"))));

        assertDoesNotThrow(() -> validator.validate(request));
    }
}