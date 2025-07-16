package com.shoppingcart.cartapp; // Tells in which folder structure (pakage: organizes classes) this file belongs to
//import statements
import org.springframework.boot.SpringApplication; // SpringApplication is a class which helps running the Spring BOot application
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; //imports @EnableMethodSecurityAnnotation

@SpringBootApplication /*  It is an annotation which combines 3 things:
 @Configuration
 @ComponentScan
 @EnableAutoCOnfiguration
 
 Which means:
 app is configurable, automatically configures and scans all the components like @Component, @Service, @Repository*/

@EnableMethodSecurity  /*This annotation tells us that we are enabling a method-level security. 
For eg: we can use @PreAuthorize() or @PostAuthorize() annotations in our services or in the controller methods. 
These will only work if @EnableMethodSecurity is there */
public class CartappApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartappApplication.class, args); /* SpringApplication.run(..) boots/starts the app
		it starts the app, loads all the configurations, sets up webserver and initializes all the components, services and repos
		It autodetects all the @RestController, @Service, etc. Connects to the DB, Sets up all REST endpoints, Runs the app at localhost:8080*/
	}
}


//String[] args enables to accept the inputs from the command line (cmd)