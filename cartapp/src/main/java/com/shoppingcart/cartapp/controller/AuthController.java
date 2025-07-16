package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.dto.AuthRequest;
import com.shoppingcart.cartapp.dto.AuthResponse;
import com.shoppingcart.cartapp.model.User;
import com.shoppingcart.cartapp.service.UserService;
import com.shoppingcart.cartapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*") // Optional: Allow any origin for frontend
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ REGISTER NEW USER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("❌ Username already taken");
        }

        // ✅ Encrypt the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // ✅ Default role if not provided
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("ROLE_USER");
        }

        User savedUser = userService.save(user);
        return ResponseEntity.ok(savedUser);
    }

    // ✅ LOGIN USER & RETURN JWT
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        System.out.println("🔐 Login attempt: " + request.getUsername());

        return userService.findByUsername(request.getUsername())
            .map(user -> {
                System.out.println("🔍 Found user: " + user.getUsername());
                System.out.println("👉 Raw password: " + request.getPassword());
                System.out.println("🔒 Stored hash: " + user.getPassword());
                System.out.println("👉 Matching: " + passwordEncoder.matches(request.getPassword(), user.getPassword())); // ✅ DEBUG

                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                    System.out.println("❌ Password does not match.");
                    return ResponseEntity.badRequest().body("❌ Invalid credentials");
                }

                // ✅ Generate token
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                return ResponseEntity.ok(new AuthResponse(token));
            })
            .orElseGet(() -> {
                System.out.println("❌ User not found");
                return ResponseEntity.status(404).body("❌ User not found");
            });
    }
}
