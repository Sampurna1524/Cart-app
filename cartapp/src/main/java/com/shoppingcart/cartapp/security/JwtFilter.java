/*This Class runs before any controller method and extracts the jwt token from the request header and then Validates the user and
 * logs the user in internally even if no session is stored and sets up their identity and role in SecurityContext
 */

package com.shoppingcart.cartapp.security; // It tells where the class is stored or to which package the class belongs

import com.shoppingcart.cartapp.util.JwtUtil; /*JwtUtil is a custom utility class which handles JWT operations like generation
of token, validation of the tokens and then extraction of the the information of the user by using the tokens */

//Jakarta Servlet API: it is used to handle HTTP requests and response in java web apps
import jakarta.servlet.FilterChain; // Used to pass the request and response to the next filter in the chain
import jakarta.servlet.ServletException; // This exception is thrown when servlet related errors occurs
import jakarta.servlet.http.HttpServletRequest; // It represents HTTP request (eg: Header, body and URI)
import jakarta.servlet.http.HttpServletResponse;// It is used to represent the HTTP response (eg: Status, headers and outputs)

import lombok.RequiredArgsConstructor;//This is used to import @RequiredArgsConstructor annotation. It is a shortcut for lambok
import org.springframework.lang.NonNull;/*To import @NoNull annotation. The method parameters, return values, and fields
 should not be null */

//Spring Security imports
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;// This represents an authentication request
import org.springframework.security.core.authority.SimpleGrantedAuthority;// It represents the role granted to the user
import org.springframework.security.core.context.SecurityContextHolder;//It holds the authentication details of the user
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;// It builds the details about the authentication request

//Spring Component import
import org.springframework.stereotype.Component;/*Marks the class as a Spring bean so that it is detected 
and managed automatically in component scanning*/

import org.springframework.web.filter.OncePerRequestFilter;/* This is a filter which runs only once per request. 
This required to extend to create a custom Jwt filter which validates the tokens before any controller method is hit*/

import java.io.IOException;// handles I/O errors
import java.util.Collections;// It is a utility class which uses the methods like emptyList() and singletonList()
import java.util.List;// It is a collection which stores ordered elements (used for storing user roles)

@Component /* This annotation makes the class a bean so that Spring can automatically detect and manage it during component scanning */
@RequiredArgsConstructor /*This annotaion is used for final fields and it automatically makes a constructor for them so that
the dependencies can be easily injected*/
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; /* JwtUtil validates the token and extracts the username and roles from the token */

    /* This function validates the user, sets the user and their roles (if the user is valid) in the SecurityContext, and then
     * pass the request to the next filter or controller.
     */
    @Override // It means that we are overriding a method from parent class
    protected void doFilterInternal( /*it is the method that we are overriding from the OncePerRequestFilter class
    This method is called every time a request comes (since we are using OncePerRequestFilter that's why this request will be
    called only once)*/
            @NonNull HttpServletRequest request, // This represents the HTTP Request
            @NonNull HttpServletResponse response, // This represents the HTTP Response
            @NonNull FilterChain filterChain // This is used to pass the request to the next filter in the filter chain or to the controller method
    ) throws ServletException, IOException { // This is used to manage the Servlet and I/O errors

        final String authHeader = request.getHeader("Authorization"); /* This is used to extract the 
        Authorization value from the from the header
        eg: Authorization: Bearer eyJhbGciOi...
         */

        if (authHeader != null && authHeader.startsWith("Bearer ")) { /* It checks if the token starts from "Bearer " or not
        as it is the standard form */
            String token = authHeader.substring(7);// Extracts the Jwt Token
            try {
                if (jwtUtil.isValid(token)) { /*If the token is valid then extract the username and the role from the token using JwtUtil
                    This checks if the token invalid or not/ expired or not. It also checks if the token is in correct format and also in
                    a correct signature*/
                    String username = jwtUtil.extractUsername(token); // The JwtUtil Extracts the Username from the token
                    String role = jwtUtil.extractRole(token);// The JwtUtil Extracts the Role from the token

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) { /*
                        The token is valid, and also the username and role are extracted using the token so now it is checked that
                        if the username is not empty and if the SecurityContextHolder (Which holds the set authentication of the active
                        user) is empty or not. For the webapp to work the above condition needs to be satisfied. So here the active user's
                        authentication data is set in the SecurityContext by storing authtoken having all these data.
                        */
                        String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();/* Checks if the 
                        role has the "ROLE_" prefix and if not then add the prefix and then convert the role into UPPERCASE */

                        List<SimpleGrantedAuthority> authorities = Collections.singletonList( /* Creates a list of type 
                        SimpleGrantedAuthority. SimpleGrantedAuthority is the class from Spring Security which represents an authority
                        or role which is granted to the user.
                        
                        authorities is the variable which reference the list of authorities
                        
                        "Collections.singletonList()" It is the utility method from the java.util.collections class. It creates an
                        immutable list which contains only one element. Here it stores the SimpleGrantedAuthority.This is used 
                        when the user has only one role */

                                new SimpleGrantedAuthority(formattedRole) /* Create a new object of SimpleGrantedAuthority which is
                                using a string passed in as "formattedRole" (Stores ROLE_ADMIN or ROLE_USER). 
                                
                                SimpleGrantedAuthority is a Spring Security Class which implements GrantedAuthority interface it is 
                                used to represent a single authority (role or permission). It is used because the Spring Security does
                                not work with the raw strings and it expects a list of GrantedAuthority objects. So when the user logs in
                                the this SimpleGrantedAuthority object is given to Spring Security os that it can later check which all things
                                or parts are the user is permitted to access.
                                
                                formattedRole Contains the string values of the roles with the added prefixes as it is the convention
                                in the Spring Security to distiguish the role from other types of permissions.
                                */
                        );

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(username, null, authorities); /* Here we are making the
                                authentication token. This authtoken will be stored in the SecurityContext.
                                This token will contain the username and the authorities details. We are not sending the password here hence it is
                                marked as null. As the authentication work is already done as the user is already signed in. And this JWT token will
                                be used in the authorization header for each of the request so that Spring can verify whether the user can access that
                                request or not.*/
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); /* This builds and adds more information 
                        in the token like session ID and IP address.*/

                        SecurityContextHolder.getContext().setAuthentication(authToken); /* This sets the authentication in the
                        SecurityContext.
                        SecurityContextHolder is the class which stores and provides access to the SecurityContext.
                        setAuthentication(authToken) is the method of SecurityContext. It is used to set the authtoken (Authentication
                        object) in the SecurityContext.
                        getContext() is a method of SecurityContextHolder which gives the access to SecurityContext so that it can store
                        authentication object (authtoken)*/

                        System.out.println("✅ JWT Authenticated: " + username + " (" + formattedRole + ")");/* Console log for seening who just
                        got authenticated */
                    }
                } else {
                    System.out.println("❌ Invalid JWT token received."); // Console log for invalid token
                }
            } catch (Exception e) {
                System.err.println("🚨 Error processing JWT: " + e.getMessage()); /* If there is any probelm while processing the token
                this will protect the app from crashing */
                // Optionally: log stack trace or redirect/clear context
            }
        }

        filterChain.doFilter(request, response);  // this is the filter chain request
        /* The request chain continues even if the token is invaliid
         * Whether or not the request is valid the request flow still continues to other filters or to be processed by the 
         * next component in the pipeline so that the errors and the invalid tokens will be handled gracefully
         */
    }
}
