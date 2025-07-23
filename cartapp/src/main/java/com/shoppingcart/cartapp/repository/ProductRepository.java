package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ✅ For pagination without filters
    Page<Product> findAll(Pageable pageable);

    // ✅ For pagination with category filter
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    // ✅ For pagination with name search
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // ✅ For pagination with both filters
    Page<Product> findByCategoryIgnoreCaseAndNameContainingIgnoreCase(String category, String name, Pageable pageable);

    // ✅ Get all categories
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();
}
