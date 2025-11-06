package com.shoppingcart.cartapp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonBackReference // ✅ Prevent infinite recursion
    private Order order;

    @ManyToOne
    private Product product;

    private int quantity;

    private double price; // unit price at time of order
}