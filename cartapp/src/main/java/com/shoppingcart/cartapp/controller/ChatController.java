package com.shoppingcart.cartapp.controller;

import com.shoppingcart.cartapp.service.GeminiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private GeminiChatService geminiChatService;

    @PostMapping("/ask")
    public Map<String, String> askChatbot(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");

        // No need for try-catch here — GeminiChatService handles exceptions internally
        String reply = geminiChatService.askGemini(message);
        return Map.of("response", reply);
    }
}
