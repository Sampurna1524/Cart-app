package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ✅ Visible only products (default view)
    Page<Product> findByVisibleTrue(Pageable pageable);

    // ✅ Visible only by category
    Page<Product> findByCategoryIgnoreCaseAndVisibleTrue(String category, Pageable pageable);

    // ✅ Visible only by name search
    Page<Product> findByNameContainingIgnoreCaseAndVisibleTrue(String name, Pageable pageable);

    // ✅ Visible only by category AND name
    Page<Product> findByCategoryIgnoreCaseAndNameContainingIgnoreCaseAndVisibleTrue(String category, String name, Pageable pageable);

    // ✅ Archived products (admin only)
    List<Product> findByVisibleFalse();
}
