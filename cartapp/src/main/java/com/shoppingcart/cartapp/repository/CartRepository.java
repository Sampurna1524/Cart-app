package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // ✅ Custom query method to find a cart by userId and status
    Optional<Cart> findByUserIdAndStatus(Long userId, String status);
}
