package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.model.User;
import com.shoppingcart.cartapp.repository.UserRepository;
import com.shoppingcart.cartapp.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // 🔐 Register a new user with encoded password
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("ROLE_USER");
        }

        User saved = userRepo.save(user);
        return ResponseEntity.ok(saved);
    }

    // 🔑 Login with JWT token response
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginData) {
        Optional<User> existing = userRepo.findByUsername(loginData.getUsername());
        if (existing.isPresent()) {
            User user = existing.get();
            if (passwordEncoder.matches(loginData.getPassword(), user.getPassword())) {
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("role", user.getRole());

                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    // ✅ Get logged-in user using JWT token
    @GetMapping("/me")
public ResponseEntity<?> getLoggedInUser(@RequestHeader("Authorization") String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
    }

    try {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId()); // ✅ ADD THIS
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("profileImageUrl", user.getProfileImageUrl());

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }
}


    // 📸 Upload profile picture
    @PostMapping("/{id}/upload-profile")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Optional<User> userOpt = userRepo.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filepath = Paths.get("uploads/profile_pics", filename);

        try {
            Files.createDirectories(filepath.getParent());
            Files.write(filepath, file.getBytes());

            User user = userOpt.get();
            user.setProfileImageUrl("/uploads/profile_pics/" + filename);
            userRepo.save(user);

            return ResponseEntity.ok(Map.of("imageUrl", user.getProfileImageUrl()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Image upload failed.");
        }
    }
}
