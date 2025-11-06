package com.shoppingcart.cartapp.repository;

import com.shoppingcart.cartapp.model.Order;
import com.shoppingcart.cartapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ✅ Existing: Get order history for a user
    List<Order> findByUserId(Long userId);

    // ✅ New: Analytics filter (date-range + user based orders)
    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.createdAt >= :date")
    List<Order> findByUserAndCreatedAtAfter(
            @Param("user") User user,
            @Param("date") LocalDateTime date
    );
}
