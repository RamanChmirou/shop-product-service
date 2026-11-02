package com.kanapa4.shop.product.validator;

import com.kanapa4.shop.product.dto.OptionGroupRequest;
import com.kanapa4.shop.product.dto.OptionValueRequest;
import com.kanapa4.shop.product.dto.ProductRequest;
import com.kanapa4.shop.product.exception.InvalidProductConfigurationException;
import com.kanapa4.shop.product.model.ProductType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class ProductRequestValidator {

    public void validate(ProductRequest request) {
        validateTypeRules(request);
        validateUniqueCodes(request);
    }

    private void validateTypeRules(ProductRequest request) {
        if (request.type() == ProductType.ELECTRONICS && !request.optionGroups().isEmpty()) {
            throw new InvalidProductConfigurationException(
                    "Products of type ELECTRONICS cannot have option groups");
        }
    }

    private void validateUniqueCodes(ProductRequest request) {
        requireUnique(
                request.optionGroups().stream().map(OptionGroupRequest::code).toList(),
                "option group codes");

        request.optionGroups().forEach(group -> requireUnique(
                group.values().stream().map(OptionValueRequest::code).toList(),
                "option value codes in group " + group.code()));
    }

    private void requireUnique(List<String> codes, String description) {
        if (new HashSet<>(codes).size() != codes.size()) {
            throw new InvalidProductConfigurationException("Duplicate " + description);
        }
    }
}