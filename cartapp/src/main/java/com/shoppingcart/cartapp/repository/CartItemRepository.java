package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.CartItem;
import com.shoppingcart.cartapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByProduct(Product product);
}
