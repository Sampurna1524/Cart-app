// Without this file the Spring Security will ignore @PreAuthorize and other annotations like this can be written but will be ignored
//This file allows Spring to consider all the security adjustments and configurations

package com.shoppingcart.cartapp.config; // This is the folder/package as to where this file is saved.

import org.springframework.context.annotation.Configuration; /* This import allows the @Configuration annotation
to make this class a Spring Configuration class. This means Spring will treat this class as a config and will 
consider all the security set ups (beans) */

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;// enables the method level security

@Configuration /* This annotation tells that this class is a configuration class 
and we can also manually define beans or special Spring security configurations (eg: enabling method security) */

@EnableMethodSecurity(prePostEnabled = true)/*It allows us to use the annotations like @PreAuthorize, @PostAuthotorize and @Security 
to be used in the Controller or Service methods. Without this even if we add those annotations it won't work

prePostEnabled = true means to allow the use of @PreAuthorize and @PostAuthorize annotations*/


public class MethodSecurityConfig {
    // We haven't defined any methods and configurations as the annotations are already enough to enable the functionalities
}
