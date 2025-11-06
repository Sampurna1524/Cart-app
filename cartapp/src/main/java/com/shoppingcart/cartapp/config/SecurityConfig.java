package com.shoppingcart.cartapp.config;

import com.shoppingcart.cartapp.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // ✅ Required for H2 console
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // ✅ JWT (no session stored)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ✅ Public pages & assets
                        .requestMatchers(
                                "/", "/index.html", "/login.html", "/signup.html", "/product.html",
                                "/cart.html", "/wishlist.html", "/orders.html", "/orders-dashboard.html",
                                "/admin.html", "/profile.html", "/about.html",
                                "/favicon.ico", "/style.css",
                                "/app.js", "/chat.js", "/cart.js", "/admin.js", "/orders-dashboard.js",
                                "/css/**", "/js/**", "/images/**", "/uploads/**"
                        ).permitAll()

                        // ✅ Public Gemini Chatbot
                        .requestMatchers("/api/chat/**").permitAll()

                        // ✅ Register/Login public
                        .requestMatchers("/users/register", "/users/login").permitAll()

                        // ✅ H2 console open
                        .requestMatchers("/h2-console/**").permitAll()

                        // ✅ Product browsing without login
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()

                        // ✅ Order Analytics allowed without auth (IMPORTANT: matches first)
                        .requestMatchers(HttpMethod.GET, "/orders/analysis/**").permitAll()

                        // 🛒 Protected APIs (except analytics above)
                        .requestMatchers("/cart/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/orders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/wishlist/**").hasAnyRole("USER", "ADMIN")

                        // 👤 User-specific authenticated actions
                        .requestMatchers("/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/*/upload-profile").authenticated()

                        // 🧑‍💼 Admin-only
                        .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/products/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/products/{id}/restock").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                // ✅ Add JWT token parser
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
