package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ✅ Get all distinct categories
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();

    // ✅ Search + Category Filter + Pagination
    @Query("SELECT p FROM Product p WHERE " +
            "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:category IS NULL OR :category = 'all' OR LOWER(p.category) = LOWER(:category))")
    Page<Product> searchAndFilterProducts(@Param("search") String search,
                                          @Param("category") String category,
                                          Pageable pageable);
}
