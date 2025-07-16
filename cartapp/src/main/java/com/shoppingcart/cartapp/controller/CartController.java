package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.model.*;
import com.shoppingcart.cartapp.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cart")
@CrossOrigin
@Slf4j
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ Create a new cart for user
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<Cart> createCart(@RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus("ACTIVE");

        return ResponseEntity.ok(cartRepository.save(cart));
    }

    // ✅ Get cart by user
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Cart> getCartByUserId(@PathVariable Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserIdAndStatus(userId, "ACTIVE");
        return cartOpt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    // ✅ Add item to cart
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{cartId}/add")
    public ResponseEntity<Cart> addItemToCart(
            @PathVariable Long cartId,
            @RequestParam Long productId,
            @RequestParam int quantity) {

        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(null);
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem existingItem = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setCart(cart);
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        return ResponseEntity.ok(cartRepository.save(cart));
    }

    // ✅ View a cart
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{cartId}")
    public ResponseEntity<Cart> getCart(@PathVariable Long cartId) {
        return cartRepository.findById(cartId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ List all carts (admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Cart>> getAllCarts() {
        return ResponseEntity.ok(cartRepository.findAll());
    }

    // ✅ Cart total
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{cartId}/total")
    public ResponseEntity<Double> getCartTotal(@PathVariable Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        return ResponseEntity.ok(total);
    }

    // ✅ Remove item
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{cartId}/remove")
    public ResponseEntity<Cart> removeItemFromCart(@PathVariable Long cartId, @RequestParam Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return ResponseEntity.ok(cartRepository.save(cart));
    }

    // ✅ Update quantity
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{cartId}/update")
    public ResponseEntity<Cart> updateCartItem(@PathVariable Long cartId, @RequestParam Long productId, @RequestParam int quantity) {
        if (quantity <= 0) {
            return ResponseEntity.badRequest().build();
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return ResponseEntity.ok(cart);
    }

    // ✅ Clear cart
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{cartId}/clear")
    public ResponseEntity<Cart> clearCart(@PathVariable Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.getItems().forEach(cartItemRepository::delete);
        cart.getItems().clear();
        return ResponseEntity.ok(cartRepository.save(cart));
    }

    // ✅ Delete cart
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> deleteCart(@PathVariable Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.getItems().forEach(cartItemRepository::delete);
        cartRepository.delete(cart);
        return ResponseEntity.ok("Cart deleted successfully");
    }

    // ✅ Checkout with address + payment method
@PreAuthorize("hasRole('USER')")
@PostMapping("/{cartId}/checkout")
public ResponseEntity<String> checkoutCart(
        @PathVariable Long cartId,
        @RequestBody CheckoutRequest request) {

    Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new RuntimeException("Cart not found"));

    double total = cart.getItems().stream()
            .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
            .sum();

    String addressSummary = String.format("%s, %s, %s - %s",
            request.getAddress().getLine(),
            request.getAddress().getCity(),
            request.getAddress().getState(),
            request.getAddress().getZip());

    String payment = request.getPaymentMethod();

    // Clear the cart
    cart.getItems().forEach(cartItemRepository::delete);
    cart.getItems().clear();
    cart.setStatus("CHECKED_OUT");
    cartRepository.save(cart);

    return ResponseEntity.ok("✅ Order placed successfully!\n" +
            "Total: ₹" + total + "\n" +
            "Shipping Address: " + addressSummary + "\n" +
            "Payment Mode: " + payment);
}

}
