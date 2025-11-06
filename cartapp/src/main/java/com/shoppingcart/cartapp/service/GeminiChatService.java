package com.shoppingcart.cartapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingcart.cartapp.model.Order;
import com.shoppingcart.cartapp.model.Product;
import com.shoppingcart.cartapp.repository.OrderRepository;
import com.shoppingcart.cartapp.repository.ProductRepository;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GeminiChatService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    private static final String GEMINI_API_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    private static final String PRODUCT_BASE_URL = "http://localhost:8080/product.html?id=";

    public GeminiChatService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /**
     ✅ Now accepts userId from ChatController
     ✅ Only fetches order history when user asks for it
     */
    public String askGemini(String userMessage, Long userId) {
        try {
            // 💾 Load all products
            List<Product> allProducts = productRepository.findAll();

            if (allProducts.isEmpty()) {
                return "⚠️ No products found in the database.";
            }

            // 🧠 Detect if the question is about order history
            boolean wantsOrderHistory = userMessage.toLowerCase()
                    .matches(".*(my order|orders|order history|previous|past|purchase).*");

            // 🧠 ORDER HISTORY (only when user asks)
            String pastOrdersSummary = "";
            if (wantsOrderHistory && userId != null) {
                List<Order> orders = orderRepository.findByUserId(userId);

                if (!orders.isEmpty()) {
                    pastOrdersSummary = orders.stream()
                            .flatMap(order -> order.getItems().stream())
                            .map(item -> String.format(
                                    "• %s (₹%.2f) Qty: %d",
                                    safe(item.getProduct().getName()),
                                    item.getProduct().getPrice(),
                                    item.getQuantity()))
                            .collect(Collectors.joining("\n"));
                } else {
                    pastOrdersSummary = "You haven't placed any orders yet.";
                }
            }

            // 🧠 PRODUCT SUMMARY (provided globally for suggestions)
            String productSummary = allProducts.stream()
                    .limit(100)
                    .map(p -> String.format(
                            "Name: %s | Price: ₹%.2f | Category: %s | ProductId: %d",
                            safe(p.getName()), p.getPrice(), safe(p.getCategory()), p.getId()))
                    .collect(Collectors.joining("\n"));

            // ✨ Prompt dynamically includes order history only if needed
            String prompt = String.format("""
                You are SmartCart AI — a shopping assistant. Answer conversationally.

                USER QUESTION:
                %s

                %s

                PRODUCT CATALOG (for suggestions):
                %s

                Rules:
                - If user asks about order history, summarize it from the section above.
                - Otherwise ignore order history completely.
                - When suggesting products, always show this format:
                  • Name (₹Price) — short reason.
                - No markdown (*, **, ##).
                """,
                    userMessage,
                    wantsOrderHistory && !pastOrdersSummary.isBlank() ?
                            "ORDER HISTORY:\n" + pastOrdersSummary + "\n" :
                            "",
                    productSummary
            );

            // ✅ Build JSON for Gemini API
            String jsonRequest = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .set("contents", objectMapper.createArrayNode().add(
                                    objectMapper.createObjectNode()
                                            .set("parts", objectMapper.createArrayNode().add(
                                                    objectMapper.createObjectNode().put("text", prompt)
                                            ))
                            ))
            );

            String url = GEMINI_API_BASE + geminiModel + ":generateContent?key=" + geminiApiKey;

            RequestBody body = RequestBody.create(jsonRequest, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode textNode = root.at("/candidates/0/content/parts/0/text");

                String formatted = formatGeminiResponse(textNode.asText());
                return injectProductLinks(formatted, allProducts).trim();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Something went wrong while contacting SmartCart AI.";
        }
    }

    private String formatGeminiResponse(String text) {
        return text.replaceAll("\\*", "").trim();
    }

    private String injectProductLinks(String response, List<Product> allProducts) {
        String result = response;
        for (Product p : allProducts) {
            if (p.getName() == null) continue;

            String regex = "(?i)" + Pattern.quote(p.getName()) + "(?=\\s|$|\\(|₹)";
            String replacement = String.format(
                    "<a href='%s%d' target='_blank' style='color:#007bff;text-decoration:none;'>%s</a>",
                    PRODUCT_BASE_URL, p.getId(), p.getName()
            );
            result = result.replaceAll(regex, Matcher.quoteReplacement(replacement));
        }
        return result;
    }

    private String safe(String value) {
        return value == null ? "N/A" : value.replace("\"", "'");
    }
}
