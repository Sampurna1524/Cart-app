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

@Configuration // Tells Spring that this class is a Configuration class
@RequiredArgsConstructor // This is a shortcut for lambok and automatically makes the constructor for final fields so that the dependencies are auto-injected
public class SecurityConfig {

    private final JwtFilter jwtFilter; /* This is a final field
    this is a filter which validates the Jwt tokens and it is injected before each request using addFilterBefore() */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { /* This is the core configuration of
        Spring Security. All the access rules, authentication rules and filers are defined here  */
        http
            .csrf(csrf -> csrf.disable()) /* Cross site request forgery (CSRF): it is used for logins (browser based login forms),
            but since we are using the REST APIs, token based security.. so we are disabling it */
            
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()) /* This is used to allow the use of <iframe> as by default Spring blocks <iframe>
                 due to security reasons so this line disable the security features which blocks them and prevents them from being
                 displayed inside an <iframe>
                 
                 x-frame-options is a http header which tells which tells the browser if the page can be displayed inside an <iframe> or not*/
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            /* The HTTP session will be stateless and won't store any session data of the user in the server.
             * There are 2 types of HTTP sessions:
             * Stateful: Those which use a session ID (sent through a cookie) to verify the user and then "remembers"
             * user, it stores all the session data so each time a page loads the session ID is sent and the user is "remembered".
             * Stateless: In these sessiions each HTTP request is treated as an independent transaction. Each time the request is 
             * made a Jwt token is used to verify the users identity. This type of session does not store any session data of the user */
            
             .authorizeHttpRequests(auth -> auth
                //Here it is written in as to which endpoints are public and which endpoints are protected

                .requestMatchers(
                    "/", "/index.html", "/login.html", "/signup.html", "/product.html", "/cart.html", "/wishlist.html",
                    "/orders.html", "/admin.html", "/profile.html", "/about.html",
                    "/favicon.ico", "/style.css", "/cart.js", "/admin.js", "/app.js",
                    "/css/**", "/js/**", "/images/**", "/uploads/**" // uploads is addeds how show all the uploaded product images by the admin
                ).permitAll() // all the frontend files are allowed to be shown without login

                // ✅ Public API endpoints
                .requestMatchers("/users/register", "/users/login").permitAll() // Signup and login are public
                .requestMatchers("/h2-console/**").permitAll() // H2 database is also allowed for everyone (Developers)
                .requestMatchers(HttpMethod.GET, "/products").permitAll() // Product viewing is also public
                .requestMatchers(HttpMethod.GET, "/products/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/products/images/**").permitAll()

                // ✅ Authenticated users (The profile page is only visible if the user is logged in else they are redirected to the login page)
                .requestMatchers("/users/me").authenticated() //The request GET /users/me requires a JWT
                .requestMatchers(HttpMethod.POST, "/users/*/upload-profile").authenticated() // Upload profile picture is also protected

                // ✅ Admin access only
                .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN") //Only admin can add a new product
                .requestMatchers(HttpMethod.POST, "/products/upload").hasRole("ADMIN") // Only admin can upload an image
                .requestMatchers(HttpMethod.DELETE, "/products/{id}").hasRole("ADMIN") // Only admin can delete the product
                .requestMatchers(HttpMethod.PATCH, "/products/{id}/restock").hasRole("ADMIN")


                // ✅ Authenticated user + admin access
                .requestMatchers("/cart/**").hasAnyRole("USER", "ADMIN") // Cart is accessible for both user and admin
                .requestMatchers("/orders/**").hasAnyRole("USER", "ADMIN") // Orders can be accessible for both users and admins
                .requestMatchers("/wishlist/**").hasAnyRole("USER", "ADMIN") // Wishlist is accessible for both th user and admin

                // 🔒 Any other endpoint requires authentication
                .anyRequest().authenticated() // any request which did not match then it will be automatically be protected and for accessing them JWT will be required
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); /* This runs JWT filter
             before the login filter but during login it does nothing as there is no authorization header. But prepares the app for the next request where the user will send the JWT token in authorization
             header for each of the next requests. After login the JWT token is generated and this JWT token is sent to all other request endpoints which are protected.
             If you don't add this then Spring will treat each request as a login attempt and will take you to login page even if the
             login was successful and even if the Jwt was generated. So by doing this we ensure that the JwtFilter is added before so that
             it extracts Jwt from the authorization header and uses it to process the request before Spring considers it as a login attempt
             and asks you to login even after a successful login.*/

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); /*It returns Spring's authentiction manager so that it 
        can authorize a user from his username and password. So basically it handles the authentication of a user using
        their credentials*/
    }

    @Bean
    //To secure the passwords
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); /*It creates an instance of BCryptPassword encoder which is a implementation
        of PasswordEncoder it uses BCrypt
        BCrypt is a hashing algorithm*/
    }
}

//AuthenticationManager, PasswordEncoder is a return type
//authenticationManager, passwordEncoder it is a function name
//AuthenticationConfiguration it is a class name which gives access to the AuthenticationManager