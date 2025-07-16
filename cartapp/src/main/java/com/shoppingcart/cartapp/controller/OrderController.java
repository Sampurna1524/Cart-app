package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.model.*;
import com.shoppingcart.cartapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
   
    private final UserRepository userRepository;

    // ✅ Place an order from cart
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/place")
    public ResponseEntity<String> placeOrder(
            @RequestParam Long cartId,
            @RequestParam Long userId,
            @RequestParam String shippingAddress,
            @RequestParam String paymentMethod) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(paymentMethod);

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getProduct().getPrice());
            total += oi.getQuantity() * oi.getPrice();
            orderItems.add(oi);
        }

        order.setItems(orderItems);
        order.setTotal(total);
        orderRepository.save(order);

        // Clear cart
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setStatus("CHECKED_OUT");
        cartRepository.save(cart);

        return ResponseEntity.ok("✅ Order placed successfully! Total: ₹" + total);
    }

    // ✅ Fetch order history for a user
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderRepository.findByUserId(userId));
    }
}
