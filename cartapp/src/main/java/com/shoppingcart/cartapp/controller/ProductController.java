package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.model.Product;
import com.shoppingcart.cartapp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@CrossOrigin
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // ✅ Get paginated & visible products (public)
    @GetMapping(produces = "application/json")
    public Page<Product> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable;

        // ✅ Handle sorting
        if ("price-asc".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by("price").ascending());
        } else if ("price-desc".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by("price").descending());
        } else if ("name-asc".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        } else if ("name-desc".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by("name").descending());
        } else {
            pageable = PageRequest.of(page, size);
        }

        // ✅ Filter only visible products
        if (category != null && !category.isBlank() && search != null && !search.isBlank()) {
            return productRepository.findByCategoryIgnoreCaseAndNameContainingIgnoreCaseAndVisibleTrue(category, search, pageable);
        } else if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryIgnoreCaseAndVisibleTrue(category, pageable);
        } else if (search != null && !search.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndVisibleTrue(search, pageable);
        } else {
            return productRepository.findByVisibleTrue(pageable);
        }
    }

    // ✅ Get product by ID (only if visible)
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .filter(Product::isVisible)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // ✅ Get all unique visible product categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = productRepository.findAll()
                .stream()
                .filter(Product::isVisible)
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    // ✅ Create new product (admin only)
    @PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Product> createProduct(@RequestBody Product product) {
    if (product.getName() == null || product.getName().isBlank() || product.getPrice() <= 0) {
        return ResponseEntity.badRequest().build();
    }

    if (product.getQuantity() < 0) {
        return ResponseEntity.badRequest().body(null); // Quantity must be >= 0
    }

    if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
        product.setImageUrl("/uploads/default.jpg");
    }

    if (product.getCategory() == null || product.getCategory().isBlank()) {
        product.setCategory("General");
    }

    Product saved = productRepository.save(product);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}


    // ✅ Delete product (admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }

        productRepository.deleteById(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    // ✅ Archive product (admin only)
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> archiveProduct(@PathVariable Long id) {
        return productRepository.findById(id).map(product -> {
            product.setVisible(false);
            productRepository.save(product);
            return ResponseEntity.ok("Product archived successfully");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found"));
    }

    // ✅ Unarchive product (admin only)
    @PutMapping("/{id}/unarchive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unarchiveProduct(@PathVariable Long id) {
        return productRepository.findById(id).map(product -> {
            product.setVisible(true);
            productRepository.save(product);
            return ResponseEntity.ok("Product unarchived successfully");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found"));
    }

    // ✅ Restock product (admin only)
    @PutMapping("/{id}/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> restockProduct(@PathVariable Long id, @RequestParam int amount) {
        return productRepository.findById(id).map(product -> {
            if (amount <= 0) {
                return ResponseEntity.badRequest().body("Restock amount must be greater than 0");
            }
            product.setQuantity(product.getQuantity() + amount);
            productRepository.save(product);
            return ResponseEntity.ok("Product restocked successfully");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found"));
    }

    // ✅ Get all archived products (admin only)
    @GetMapping("/archived")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> getArchivedProducts() {
        return productRepository.findByVisibleFalse();
    }
}
