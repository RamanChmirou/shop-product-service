package com.kanapa4.shop.product.validator;

import com.kanapa4.shop.product.dto.SelectedOptionRequest;
import com.kanapa4.shop.product.exception.InvalidProductConfigurationException;
import com.kanapa4.shop.product.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.kanapa4.shop.product.testdata.ProductFixtures.laptop;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductConfigurationValidatorTest {

    private final ProductConfigurationValidator validator = new ProductConfigurationValidator();

    private final Product laptop = laptop();

    private static SelectedOptionRequest option(String groupCode, String valueCode) {
        return new SelectedOptionRequest(groupCode, valueCode);
    }

    @Test
    void validate_AllRequiredGroupsSelected_DoesNotThrow() {
        List<SelectedOptionRequest> selected = List.of(option("PROCESSOR", "I7"), option("RAM", "RAM_32"));

        assertDoesNotThrow(() -> validator.validate(laptop, selected));
    }

    @Test
    void validate_OptionalMultiGroupWithTwoValues_DoesNotThrow() {
        List<SelectedOptionRequest> selected = List.of(
                option("PROCESSOR", "I5"), option("RAM", "RAM_16"),
                option("ACCESSORIES", "CASE"), option("ACCESSORIES", "CHARGER"));

        assertDoesNotThrow(() -> validator.validate(laptop, selected));
    }

    @Test
    void validate_MissingRequiredGroup_ThrowsException() {
        List<SelectedOptionRequest> selected = List.of(option("PROCESSOR", "I7"));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(laptop, selected));
    }

    @Test
    void validate_EmptySelectionWhenRequiredGroupsExist_ThrowsException() {
        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(laptop, List.of()));
    }

    @Test
    void validate_UnknownGroup_ThrowsException() {
        List<SelectedOptionRequest> selected = List.of(
                option("PROCESSOR", "I7"), option("RAM", "RAM_32"), option("COLOR", "BLACK"));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(laptop, selected));
    }

    @Test
    void validate_UnknownValueInGroup_ThrowsException() {
        List<SelectedOptionRequest> selected = List.of(option("PROCESSOR", "I99"), option("RAM", "RAM_32"));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(laptop, selected));
    }

    @Test
    void validate_TwoValuesInSingleGroup_ThrowsException() {
        List<SelectedOptionRequest> selected = List.of(
                option("PROCESSOR", "I5"), option("PROCESSOR", "I7"), option("RAM", "RAM_32"));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(laptop, selected));
    }

    @Test
    void validate_SameOptionSelectedTwice_ThrowsException() {
        List<SelectedOptionRequest> selected = List.of(
                option("PROCESSOR", "I7"), option("RAM", "RAM_32"),
                option("ACCESSORIES", "CASE"), option("ACCESSORIES", "CASE"));

        assertThrows(InvalidProductConfigurationException.class, () -> validator.validate(laptop, selected));
    }
}