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
            // 🔒 Disable CSRF for REST APIs
            .csrf(csrf -> csrf.disable())

            // 🧩 Allow H2 Console (disable frame options)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            // 🚫 Stateless (JWT only)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ✅ Public static frontend files
                .requestMatchers(
                    "/", "/index.html", "/login.html", "/signup.html", "/product.html", "/cart.html",
                    "/wishlist.html", "/orders.html", "/admin.html", "/profile.html", "/about.html",
                    "/favicon.ico", "/style.css", "/chat.js", "/cart.js", "/admin.js", "/app.js",
                    "/css/**", "/js/**", "/images/**", "/uploads/**"
                ).permitAll()

                // ✅ Allow chatbot backend API (Gemini)
                .requestMatchers("/api/chat/**").permitAll()

                // ✅ Public authentication + product APIs
                .requestMatchers("/users/register", "/users/login").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/products/**").permitAll()

                // 👤 User endpoints
                .requestMatchers("/users/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/users/*/upload-profile").authenticated()

                // 🛒 Cart, orders, wishlist — requires login
                .requestMatchers("/cart/**", "/orders/**", "/wishlist/**")
                    .hasAnyRole("USER", "ADMIN")

                // 🧑‍💼 Admin endpoints
                .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/products/upload").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/products/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/products/{id}/restock").hasRole("ADMIN")

                // 🚨 Everything else
                .anyRequest().authenticated()
            )

            // ✅ Add JWT filter before UsernamePasswordAuthenticationFilter
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
