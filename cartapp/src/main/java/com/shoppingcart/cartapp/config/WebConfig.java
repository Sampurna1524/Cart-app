package com.shoppingcart.cartapp.config; // Tells to which folder or package does the file belong to.

import org.springframework.context.annotation.Configuration; // Allows the use of @Configuration annotaion
import org.springframework.lang.NonNull; // Imports @Nonull annotation. The Method parameters, Return value or fields should not be null
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry; /* It imports a class ResourceHandlerRegistry
from Spring MVC. It is used for registering custom resourse handlers for serving static resourses (Files in static folder) in a Spring
MVC application */
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;/*To implement the WebMvcCofigurer interface. 
We implement this interface in over Configuration classes only when we want to change and customize the default configurations
as provided by Spring MVC.*/

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration // It tells Spring that this class is a configuration class so that it considers any security configurations or beans added and does not ignore them 
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) { /*since we want to serve static files (here in this project uploads) in this class
        we will implement addResourceHandlers(..) function (method) in this class*/
        // Serve files from ./uploads/ directory on disk
        Path uploadDir = Paths.get("uploads"); // makes the path of uploads folder inside the project root
        String uploadPath = uploadDir.toFile().getAbsolutePath();// getAbsolutePath() makes the entire path of the uploads folder

        // This handles the request from the frontend for an image.
        registry
            .addResourceHandler("/uploads/**") /*if the fronend requests an image with the path with this pattern
            inside the /uploads/...*/
            .addResourceLocations("file:" + uploadPath + "/");/*then the backend will directly show the file with the same file path in the
            requested location*/

    }
}


/*eg: if we upload a file as uploads/mouse123.jpg
 * then if we go to http://localhost:8080/uploads/mouse123.jpg the image will be visible.
*/