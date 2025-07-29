package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

// ✅ Get all distinct categories (from non-archived products only)
@Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.archived = false")
 List<String> findDistinctCategories();

 // ✅ Search + Category Filter + Pagination (only non-archived)
 @Query("SELECT p FROM Product p WHERE " +
   "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
   "(:category IS NULL OR :category = 'all' OR LOWER(p.category) = LOWER(:category)) AND " +
   "p.archived = false")
 Page<Product> searchAndFilterProducts(@Param("search") String search,
          @Param("category") String category,
          Pageable pageable);

 // ✅ Get all non-archived products
 List<Product> findByArchivedFalse();

 // *** New: Get all archived products ***
 List<Product> findByArchivedTrue();
}
