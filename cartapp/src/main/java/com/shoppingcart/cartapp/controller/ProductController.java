package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.dto.RestockRequest;
import com.shoppingcart.cartapp.model.Product;
import com.shoppingcart.cartapp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@CrossOrigin
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // ✅ Get products with search, filter, sort, pagination (non-archived only)
    @GetMapping(produces = "application/json")
    public ResponseEntity<Page<Product>> getProductsWithFilters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "id-asc") String sort
    ) {
        Pageable pageable;

        switch (sort) {
            case "price-asc":
                pageable = PageRequest.of(page, size, Sort.by("price").ascending());
                break;
            case "price-desc":
                pageable = PageRequest.of(page, size, Sort.by("price").descending());
                break;
            case "name-desc":
                pageable = PageRequest.of(page, size, Sort.by("name").descending());
                break;
            case "name-asc":
                pageable = PageRequest.of(page, size, Sort.by("name").ascending());
                break;
            default:
                pageable = PageRequest.of(page, size, Sort.by("id").ascending());
                break;
        }

        Page<Product> result = productRepository.searchAndFilterProducts(search, category, pageable);
        return ResponseEntity.ok(result);
    }

    // ✅ Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // ✅ Create new product (admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        if (product.getName() == null || product.getName().isBlank() || product.getPrice() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            product.setImageUrl("/uploads/default.jpg");
        }

        if (product.getQuantity() < 0) {
            product.setQuantity(0);
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ✅ Delete product by ID (admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }

        productRepository.deleteById(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    // ✅ Restock product (admin only)
    @PatchMapping("/{id}/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restockProduct(
            @PathVariable Long id,
            @RequestBody RestockRequest request) {

        if (request == null || request.getAmount() <= 0) {
            return ResponseEntity.badRequest().body("Amount must be greater than 0");
        }

        return productRepository.findById(id).map(product -> {
            product.setQuantity(product.getQuantity() + request.getAmount());
            productRepository.save(product);
            return ResponseEntity.ok("Product restocked successfully");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found"));
    }

    // ✅ Get all unique product categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = productRepository.findDistinctCategories()
                .stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }

    // ✅ Get all non-archived products (used by user index page)
    @GetMapping("/all")
    public List<Product> getAllProducts() {
        return productRepository.findByArchivedFalse();
    }

    // *** NEW: Get all archived products (admin only) ***
    @GetMapping("/archived")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> getArchivedProducts() {
        return productRepository.findByArchivedTrue();
    }

    // ✅ Toggle archive/unarchive a product (admin only)
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleArchive(@PathVariable Long id) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }

        Product product = optionalProduct.get();
        product.setArchived(!product.isArchived());
        productRepository.save(product);

        String message = product.isArchived()
                ? "Product archived successfully"
                : "Product unarchived successfully";

        return ResponseEntity.ok(message);
    }
}
