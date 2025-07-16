package com.shoppingcart.cartapp.service;

import com.shoppingcart.cartapp.model.User;
import com.shoppingcart.cartapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String inputUsername) throws UsernameNotFoundException {
        // ✅ Use a new variable to avoid mutation
        String trimmedUsername = inputUsername.trim();

        User user = userRepo.findByUsername(trimmedUsername)
            .orElseThrow(() -> new UsernameNotFoundException("❌ User not found: " + trimmedUsername));

        // ✅ Debug logs (optional)
        System.out.println("✅ Auth User: " + user.getUsername());
        System.out.println("🔒 Password: " + user.getPassword());
        System.out.println("🛡️ Role: " + user.getRole());

        // ✅ Normalize role
        String role = user.getRole();
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring(5); // "ROLE_ADMIN" -> "ADMIN"
        }

        // ✅ Build UserDetails object
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(role != null ? role.toUpperCase() : "USER")
                .build();
    }
}
