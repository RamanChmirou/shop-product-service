package com.kanapa4.shop.product.mapper;

import com.kanapa4.shop.product.dto.OptionGroupDto;
import com.kanapa4.shop.product.dto.OptionGroupRequest;
import com.kanapa4.shop.product.dto.OptionValueDto;
import com.kanapa4.shop.product.dto.OptionValueRequest;
import com.kanapa4.shop.product.dto.PriceDto;
import com.kanapa4.shop.product.dto.PriceRequest;
import com.kanapa4.shop.product.dto.ProductConfigurationDto;
import com.kanapa4.shop.product.dto.ProductDto;
import com.kanapa4.shop.product.dto.ProductRequest;
import com.kanapa4.shop.product.dto.ProductSummaryDto;
import com.kanapa4.shop.product.model.OptionGroup;
import com.kanapa4.shop.product.model.OptionValue;
import com.kanapa4.shop.product.model.Price;
import com.kanapa4.shop.product.model.Product;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "price", source = "basePrice")
    ProductSummaryDto toSummary(Product product);

    @Mapping(target = "price", source = "basePrice")
    ProductDto toDto(Product product);

    @Mapping(target = "productId", source = "id")
    @Mapping(target = "productName", source = "name")
    ProductConfigurationDto toConfiguration(Product product);

    PriceDto toPriceDto(Price price);

    OptionGroupDto toOptionGroupDto(OptionGroup group);

    OptionValueDto toOptionValueDto(OptionValue value);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "basePrice", source = "price")
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "basePrice", source = "price")
    void update(@MappingTarget Product product, ProductRequest request);

    Price toPrice(PriceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "position", ignore = true)
    OptionGroup toOptionGroup(OptionGroupRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "optionGroup", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "priceDelta", source = "priceDelta", defaultValue = "0")
    OptionValue toOptionValue(OptionValueRequest request);

    @AfterMapping
    default void linkOptionGroups(@MappingTarget Product product) {
        int groupOrder = 0;
        for (OptionGroup group : product.getOptionGroups()) {
            group.setProduct(product);
            group.setPosition(groupOrder++);
            int valueOrder = 0;
            for (OptionValue value : group.getValues()) {
                value.setOptionGroup(group);
                value.setPosition(valueOrder++);
            }
        }
    }
}