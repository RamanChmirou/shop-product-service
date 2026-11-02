package com.kanapa4.shop.product.testdata;

import com.kanapa4.shop.product.model.OptionGroup;
import com.kanapa4.shop.product.model.OptionValue;
import com.kanapa4.shop.product.model.Price;
import com.kanapa4.shop.product.model.Product;
import com.kanapa4.shop.product.model.ProductType;
import com.kanapa4.shop.product.model.SelectionType;
import com.kanapa4.shop.product.model.VatRate;

import java.math.BigDecimal;
import java.util.UUID;

public final class ProductFixtures {

    public static final UUID LAPTOP_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    private ProductFixtures() {
    }

    public static Price price(String amount) {
        return new Price(new BigDecimal(amount), "PLN", VatRate.VAT_23);
    }

    public static Product laptop() {
        Product product = new Product();
        product.setId(LAPTOP_ID);
        product.setName("Laptop Pro 15");
        product.setType(ProductType.COMPUTER);
        product.setBasePrice(price("3999.00"));
        product.setActive(true);

        product.addOptionGroup(group("PROCESSOR", "Processor", SelectionType.SINGLE, true, 1,
                value("I5", "Intel Core i5", "0.00", 1),
                value("I7", "Intel Core i7", "800.00", 2)));
        product.addOptionGroup(group("RAM", "Memory (RAM)", SelectionType.SINGLE, true, 2,
                value("RAM_16", "16 GB", "0.00", 1),
                value("RAM_32", "32 GB", "400.00", 2)));
        product.addOptionGroup(group("ACCESSORIES", "Accessories", SelectionType.MULTI, false, 3,
                value("CASE", "Protective case", "49.00", 1),
                value("CHARGER", "Fast charger", "99.00", 2)));
        return product;
    }

    private static OptionGroup group(String code, String name, SelectionType selectionType,
                                     boolean required, int position, OptionValue... values) {
        OptionGroup group = new OptionGroup();
        group.setCode(code);
        group.setName(name);
        group.setSelectionType(selectionType);
        group.setRequired(required);
        group.setPosition(position);
        for (OptionValue value : values) {
            group.addValue(value);
        }
        return group;
    }

    private static OptionValue value(String code, String label, String priceDelta, int position) {
        OptionValue value = new OptionValue();
        value.setCode(code);
        value.setLabel(label);
        value.setPriceDelta(new BigDecimal(priceDelta));
        value.setPosition(position);
        return value;
    }
}