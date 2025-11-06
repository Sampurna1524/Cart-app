package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.model.*;
import com.shoppingcart.cartapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@CrossOrigin
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // ✅ PLACE ORDER
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
            Product product = ci.getProduct();

            if (product.getQuantity() < ci.getQuantity()) {
                return ResponseEntity.badRequest().body("❌ Not enough stock for: " + product.getName());
            }

            product.setQuantity(product.getQuantity() - ci.getQuantity());
            productRepository.save(product);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(product.getPrice());

            total += oi.getQuantity() * oi.getPrice();
            orderItems.add(oi);
        }

        order.setItems(orderItems);
        order.setTotal(total);
        orderRepository.save(order);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setStatus("CHECKED_OUT");
        cartRepository.save(cart);

        return ResponseEntity.ok("✅ Order placed successfully! Total: ₹" + total);
    }

    // ✅ ORDER HISTORY
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderRepository.findByUserId(userId));
    }

    // ✅ ORDER ANALYTICS (Dashboard charts)
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/analysis")
    public ResponseEntity<?> getOrderAnalytics(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "all") String filter
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders;

        switch (filter) {
            case "7d" -> orders = orderRepository.findByUserAndCreatedAtAfter(user, LocalDateTime.now().minusDays(7));
            case "30d" -> orders = orderRepository.findByUserAndCreatedAtAfter(user, LocalDateTime.now().minusDays(30));
            default -> orders = orderRepository.findByUserId(userId); // ✅ ALL orders (no date filter)
        }

        double totalSpent = orders.stream().mapToDouble(Order::getTotal).sum();
        int totalOrders = orders.size();

        Map<String, Long> categoryCount = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory(),
                        Collectors.counting()
                ));

        return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "totalSpent", totalSpent,
                "categories", categoryCount
        ));
    }
}
