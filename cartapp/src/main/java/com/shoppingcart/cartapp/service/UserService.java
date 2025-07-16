package com.shoppingcart.cartapp.service;

import com.shoppingcart.cartapp.model.User;
import com.shoppingcart.cartapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User save(User user) {
        // ✅ Trim all fields to remove leading/trailing whitespace
        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim());

        // ✅ Ensure role starts with "ROLE_" (Spring Security convention)
        if (!user.getRole().startsWith("ROLE_")) {
            user.setRole("ROLE_" + user.getRole().toUpperCase());  // eg. "admin" → "ROLE_ADMIN"
        }

        // ✅ Encode password only if not already encoded
        if (!user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username.trim());
    }
}
