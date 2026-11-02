package com.kanapa4.shop.product.repository;

import com.kanapa4.shop.product.model.Product;
import com.kanapa4.shop.product.model.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAllByActiveTrue(Pageable pageable);

    Page<Product> findAllByTypeAndActiveTrue(ProductType type, Pageable pageable);
}