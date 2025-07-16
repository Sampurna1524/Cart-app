package com.shoppingcart.cartapp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double price;

    @Builder.Default
    @Column(length = 512)
    private String imageUrl = "/uploads/default.jpg";

    @Builder.Default
    @Column(length = 100)
    private String category = "General";

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private int quantity = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean visible = true;
}
