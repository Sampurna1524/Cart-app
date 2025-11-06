package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.model.User;
import com.shoppingcart.cartapp.repository.UserRepository;
import com.shoppingcart.cartapp.service.GeminiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiChatService geminiChatService;
    private final UserRepository userRepository;

    @PostMapping("/ask")
    public Map<String, String> askChatbot(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails principal  // ✅ Extract JWT user if present
    ) {

        String message = (String) payload.get("message");
        Long userId = null;

        // ✅ 1. Check if userId sent by frontend
        if (payload.get("userId") != null) {
            userId = Long.valueOf(payload.get("userId").toString());
        }

        // ✅ 2. If JWT exists, override with logged in user
        if (principal != null) {
            String username = principal.getUsername();
            userId = userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElse(userId);  // fallback to passed userId
        }

        System.out.println("🤖 Chat Request: message=" + message + " | userId=" + userId);

        // ✅ Pass to Gemini with userId (for order history context)
        String reply = geminiChatService.askGemini(message, userId);

        return Map.of("response", reply);
    }
}
