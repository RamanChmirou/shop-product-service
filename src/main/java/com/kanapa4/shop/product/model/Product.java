package com.kanapa4.shop.product.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductType type;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "base_price", nullable = false, precision = 19, scale = 2)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "currency", nullable = false, length = 3)),
            @AttributeOverride(name = "vatRate",
                    column = @Column(name = "vat_rate", nullable = false, length = 16))
    })
    private Price basePrice;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> attributes = new HashMap<>();

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<OptionGroup> optionGroups = new ArrayList<>();

    public void addOptionGroup(OptionGroup group) {
        optionGroups.add(group);
        group.setProduct(this);
    }
}
