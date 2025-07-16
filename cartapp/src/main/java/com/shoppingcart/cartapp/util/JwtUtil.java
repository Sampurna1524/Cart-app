package com.shoppingcart.cartapp.util;// This is the folder structure/ package in which the file is stored in

import io.jsonwebtoken.*;/* This is the java library which is used to import all the classes for working with JWTs
"*" means that we will import all the classes in jsonwebtoken library */

import io.jsonwebtoken.security.Keys; /* This is a java library which imports secure keys for signing our JWTs. 
The keys are used for proving that the Jwts are valid and has not been changed*/
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final String SECRET = "smartcart_secret_key_which_is_long_enough_to_be_secure_12345"; // ✅ At least 32 chars
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ✅ Generate token with username & role
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ Extract role from token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // ✅ Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = getAllClaims(token);
        return resolver.apply(claims);
    }

    // ✅ Validate token (signature + expiry)
    public boolean isValid(String token) {
        try {
            getAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("❌ Invalid token: " + e.getMessage());
            return false;
        }
    }

    // ✅ Internal: extract all claims
    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
