package com.shoppingcart.cartapp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users") // ✅ avoids reserved keyword conflict
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // ✅ Role used for Spring Security (must start with "ROLE_")
    @Column(nullable = false)
    private String role = "ROLE_USER"; // Default to normal user

    // 🖼️ Profile image URL (relative path)
    @Column(name = "profile_image_url")
    private String profileImageUrl;

}
