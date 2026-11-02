package com.kanapa4.shop.product.validator;

import com.kanapa4.shop.product.dto.SelectedOptionRequest;
import com.kanapa4.shop.product.exception.InvalidProductConfigurationException;
import com.kanapa4.shop.product.model.OptionGroup;
import com.kanapa4.shop.product.model.OptionValue;
import com.kanapa4.shop.product.model.Product;
import com.kanapa4.shop.product.model.SelectionType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductConfigurationValidator {

    public void validate(Product product, List<SelectedOptionRequest> selectedOptions) {
        Map<String, List<String>> valueCodesByGroup = selectedOptions.stream()
                .collect(Collectors.groupingBy(
                        SelectedOptionRequest::groupCode,
                        Collectors.mapping(SelectedOptionRequest::valueCode, Collectors.toList())));

        validateKnownGroups(product, valueCodesByGroup.keySet());
        product.getOptionGroups().forEach(group ->
                validateGroup(group, valueCodesByGroup.getOrDefault(group.getCode(), List.of())));
    }

    private void validateKnownGroups(Product product, Set<String> selectedGroupCodes) {
        Set<String> knownCodes = product.getOptionGroups().stream()
                .map(OptionGroup::getCode)
                .collect(Collectors.toSet());

        Set<String> unknownCodes = new HashSet<>(selectedGroupCodes);
        unknownCodes.removeAll(knownCodes);
        if (!unknownCodes.isEmpty()) {
            throw new InvalidProductConfigurationException("Unknown option groups: " + unknownCodes);
        }
    }

    private void validateGroup(OptionGroup group, List<String> selectedValueCodes) {
        String groupCode = group.getCode();

        if (group.isRequired() && selectedValueCodes.isEmpty()) {
            throw new InvalidProductConfigurationException(
                    "Option group '" + groupCode + "' is required");
        }
        if (group.getSelectionType() == SelectionType.SINGLE && selectedValueCodes.size() > 1) {
            throw new InvalidProductConfigurationException(
                    "Option group '" + groupCode + "' allows only one value");
        }
        if (new HashSet<>(selectedValueCodes).size() != selectedValueCodes.size()) {
            throw new InvalidProductConfigurationException(
                    "Duplicate values selected in option group '" + groupCode + "'");
        }

        Set<String> availableCodes = group.getValues().stream()
                .map(OptionValue::getCode)
                .collect(Collectors.toSet());
        if (!availableCodes.containsAll(selectedValueCodes)) {
            throw new InvalidProductConfigurationException(
                    "Unknown value selected in option group '" + groupCode + "'");
        }
    }
}