package com.kanapa4.shop.product;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest {

    private static final String LAPTOP_ID = "11111111-0000-0000-0000-000000000001";
    private static final String SMARTPHONE_ID = "22222222-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;


    private static String electronicsJson(String name, String amount, String power) {
        return """
                {
                  "name": "%s",
                  "description": "Integration test product",
                  "type": "ELECTRONICS",
                  "price": { "amount": %s, "currency": "PLN", "vatRate": "VAT_23" },
                  "attributes": { "power": "%s" }
                }
                """.formatted(name, amount, power);
    }

    private static String computerJson(String name, String valuesJson) {
        return """
                {
                  "name": "%s",
                  "description": "Integration test computer",
                  "type": "COMPUTER",
                  "price": { "amount": 3000.00, "currency": "PLN", "vatRate": "VAT_23" },
                  "optionGroups": [
                    {
                      "code": "COLOR",
                      "name": "Color",
                      "selectionType": "SINGLE",
                      "required": true,
                      "values": %s
                    }
                  ]
                }
                """.formatted(name, valuesJson);
    }

    private static String values(String... codes) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"code\":\"").append(codes[i])
                    .append("\",\"label\":\"").append(codes[i])
                    .append("\",\"priceDelta\":0}");
        }
        return builder.append("]").toString();
    }

    private static String selection(String... pairs) {
        StringBuilder builder = new StringBuilder("{\"selectedOptions\":[");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"groupCode\":\"").append(pairs[i])
                    .append("\",\"valueCode\":\"").append(pairs[i + 1]).append("\"}");
        }
        return builder.append("]}").toString();
    }

    private static String batchItem(String productId, String... pairs) {
        StringBuilder options = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                options.append(",");
            }
            options.append("{\"groupCode\":\"").append(pairs[i])
                    .append("\",\"valueCode\":\"").append(pairs[i + 1]).append("\"}");
        }
        return "{\"productId\":\"%s\",\"selectedOptions\":[%s]}".formatted(productId, options);
    }

    private static String batch(String... items) {
        return "{\"items\":[" + String.join(",", items) + "]}";
    }

    private String createProduct(String json) throws Exception {
        String body = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void getAll_SeededDatabase_ReturnFirstPageSortedByName() throws Exception {
        mockMvc.perform(get("/products").param("page", "0").param("size", "3").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Gaming Tower"))
                .andExpect(jsonPath("$.content[1].name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.page.size").value(3))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(7)));
    }

    @Test
    void getAll_SizeOne_ReturnOneElementAndManyPages() throws Exception {
        mockMvc.perform(get("/products").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(greaterThanOrEqualTo(7)));
    }

    @Test
    void getAll_UnknownSortProperty_Return400() throws Exception {
        mockMvc.perform(get("/products").param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("foo")));
    }

    @Test
    void getByType_Smartphone_ReturnOnlySmartphones() throws Exception {
        mockMvc.perform(get("/products").param("type", "SMARTPHONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.type != 'SMARTPHONE')]").isEmpty());
    }

    @Test
    void getById_Laptop_ReturnProductWithPriceAndJsonAttributes() throws Exception {
        mockMvc.perform(get("/products/{id}", LAPTOP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.type").value("COMPUTER"))
                .andExpect(jsonPath("$.price.amount").value(3999.00))
                .andExpect(jsonPath("$.price.currency").value("PLN"))
                .andExpect(jsonPath("$.price.vatRate").value("VAT_23"))
                .andExpect(jsonPath("$.attributes.screen").value("15.6 inch"));
    }

    @Test
    void getById_UnknownId_Return404() throws Exception {
        mockMvc.perform(get("/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getConfiguration_Laptop_ReturnGroupsAndValuesInPositionOrder() throws Exception {
        mockMvc.perform(get("/products/{id}/configuration", LAPTOP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optionGroups.length()").value(2))
                .andExpect(jsonPath("$.optionGroups[0].code").value("PROCESSOR"))
                .andExpect(jsonPath("$.optionGroups[0].selectionType").value("SINGLE"))
                .andExpect(jsonPath("$.optionGroups[0].required").value(true))
                .andExpect(jsonPath("$.optionGroups[0].values[0].code").value("I5"))
                .andExpect(jsonPath("$.optionGroups[0].values[1].code").value("I7"))
                .andExpect(jsonPath("$.optionGroups[0].values[2].code").value("I9"))
                .andExpect(jsonPath("$.optionGroups[1].code").value("RAM"));
    }
    @Test
    void quote_LaptopWithUpgrades_ReturnBasePricePlusSurcharges() throws Exception {
        mockMvc.perform(post("/products/{id}/price-quote", LAPTOP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection("PROCESSOR", "I7", "RAM", "RAM_32")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice.amount").value(5199.00))
                .andExpect(jsonPath("$.unitPrice.currency").value("PLN"))
                .andExpect(jsonPath("$.selectedOptions.length()").value(2));
    }

    @Test
    void quote_SmartphoneWithAccessories_SumsAllSurcharges() throws Exception {
        mockMvc.perform(post("/products/{id}/price-quote", SMARTPHONE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection("COLOR", "BLUE", "BATTERY_CAPACITY", "MAH_5000",
                                "ACCESSORIES", "CASE", "ACCESSORIES", "CHARGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice.amount").value(2847.00));
    }

    @Test
    void quote_MissingRequiredGroup_Return400() throws Exception {
        mockMvc.perform(post("/products/{id}/price-quote", LAPTOP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection("PROCESSOR", "I7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void quote_TwoValuesInSingleGroup_Return400() throws Exception {
        mockMvc.perform(post("/products/{id}/price-quote", LAPTOP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection("PROCESSOR", "I5", "PROCESSOR", "I7", "RAM", "RAM_16")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quote_UnknownProduct_Return404() throws Exception {
        mockMvc.perform(post("/products/{id}/price-quote", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selection("PROCESSOR", "I7")))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_ElectronicsWithOptionGroups_Return400() throws Exception {
        String json = """
                {
                  "name": "Zzz Broken Product",
                  "type": "ELECTRONICS",
                  "price": { "amount": 99.00, "currency": "PLN", "vatRate": "VAT_23" },
                  "optionGroups": [
                    { "code": "COLOR", "name": "Color", "selectionType": "SINGLE", "required": true,
                      "values": [ { "code": "BLACK", "label": "Black", "priceDelta": 0 } ] }
                  ]
                }
                """;

        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productLifecycle_CreateReadUpdateDelete_WorksEndToEnd() throws Exception {
        String id = createProduct(electronicsJson("Zzz Integration Speaker", "199.00", "20 W"));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Zzz Integration Speaker"))
                .andExpect(jsonPath("$.price.amount").value(199.00))
                .andExpect(jsonPath("$.attributes.power").value("20 W"));

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(electronicsJson("Zzz Integration Speaker Max", "249.00", "30 W")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Zzz Integration Speaker Max"))
                .andExpect(jsonPath("$.price.amount").value(249.00))
                .andExpect(jsonPath("$.attributes.power").value("30 W"));

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_ReplaceOptionValuesInSameGroup_ReplacesWithoutUniqueIndexViolation() throws Exception {
        String id = createProduct(computerJson("Zzz Integration Computer", values("BLACK", "WHITE")));

        mockMvc.perform(get("/products/{id}/configuration", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optionGroups.length()").value(1))
                .andExpect(jsonPath("$.optionGroups[0].values.length()").value(2));

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computerJson("Zzz Integration Computer", values("RED", "GREEN", "BLUE"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/{id}/configuration", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optionGroups.length()").value(1))
                .andExpect(jsonPath("$.optionGroups[0].code").value("COLOR"))
                .andExpect(jsonPath("$.optionGroups[0].values.length()").value(3))
                .andExpect(jsonPath("$.optionGroups[0].values[0].code").value("RED"))
                .andExpect(jsonPath("$.optionGroups[0].values[1].code").value("GREEN"))
                .andExpect(jsonPath("$.optionGroups[0].values[2].code").value("BLUE"));

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_UnknownProduct_Return404() throws Exception {
        mockMvc.perform(delete("/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void quoteAll_LaptopAndSmartphone_ReturnPricesInRequestOrder() throws Exception {
        String body = batch(
                batchItem(LAPTOP_ID, "PROCESSOR", "I7", "RAM", "RAM_32"),
                batchItem(SMARTPHONE_ID, "COLOR", "BLUE", "BATTERY_CAPACITY", "MAH_5000",
                        "ACCESSORIES", "CASE", "ACCESSORIES", "CHARGER"));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotes.length()").value(2))
                .andExpect(jsonPath("$.quotes[0].productId").value(LAPTOP_ID))
                .andExpect(jsonPath("$.quotes[0].unitPrice.amount").value(5199.00))
                .andExpect(jsonPath("$.quotes[1].productId").value(SMARTPHONE_ID))
                .andExpect(jsonPath("$.quotes[1].unitPrice.amount").value(2847.00));
    }

    @Test
    void quoteAll_SameLaptopTwoConfigurations_ReturnSeparateQuotes() throws Exception {
        String body = batch(
                batchItem(LAPTOP_ID, "PROCESSOR", "I7", "RAM", "RAM_32"),
                batchItem(LAPTOP_ID, "PROCESSOR", "I7", "RAM", "RAM_16"));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotes[0].unitPrice.amount").value(5199.00))
                .andExpect(jsonPath("$.quotes[1].unitPrice.amount").value(4799.00));
    }

    @Test
    void quoteAll_OneUnknownProduct_Return404ForWholeRequest() throws Exception {
        String body = batch(
                batchItem(LAPTOP_ID, "PROCESSOR", "I7", "RAM", "RAM_32"),
                batchItem(UUID.randomUUID().toString()));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void quoteAll_OneInvalidSelection_Return400ForWholeRequest() throws Exception {
        String body = batch(
                batchItem(LAPTOP_ID, "PROCESSOR", "I7", "RAM", "RAM_32"),
                batchItem(LAPTOP_ID, "PROCESSOR", "I7"));

        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void quoteAll_EmptyItems_Return400() throws Exception {
        mockMvc.perform(post("/products/price-quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch()))
                .andExpect(status().isBadRequest());
    }
}