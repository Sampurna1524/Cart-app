package com.shoppingcart.cartapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingcart.cartapp.model.Product;
import com.shoppingcart.cartapp.repository.ProductRepository;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    // ✅ Change this if your frontend URL differs
    private static final String PRODUCT_BASE_URL = "http://localhost:8080/product.html?id=";

    public GeminiChatService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String askGemini(String userMessage) {
        try {
            // 🧠 Step 1: Get all products
            List<Product> allProducts = productRepository.findAll();
            if (allProducts == null || allProducts.isEmpty()) {
                return "⚠️ No products found in the database. Please add some first!";
            }

            // 🧠 Step 2: Prepare product summary
            String productSummary = allProducts.stream()
                    .limit(100)
                    .map(p -> String.format("Name: %s | Price: ₹%.2f | Category: %s",
                            safe(p.getName()), p.getPrice(), safe(p.getCategory())))
                    .collect(Collectors.joining("\n"));

            // 🧠 Step 3: Construct Gemini prompt
            String prompt = String.format("""
                    You are SmartCart AI — a helpful shopping assistant for an e-commerce website.
                    Use the following product data to recommend, search, or compare products.
                    Be concise, friendly, and only refer to these products.

                    PRODUCT CATALOG:
                    %s

                    USER QUESTION:
                    %s

                    Rules:
                    - Recommend products relevant to user's query.
                    - If comparing, show differences in price and category.
                    - If searching by budget, list products under that price.
                    - Respond conversationally but factually.
                    - Avoid Markdown (*, **, #).
                    - Format like:
                      • Product Name (₹Price) — short comment.
                    """, productSummary, userMessage);

            // ✅ Step 4: JSON request body
            String jsonRequest = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .set("contents", objectMapper.createArrayNode().add(
                                    objectMapper.createObjectNode()
                                            .set("parts", objectMapper.createArrayNode().add(
                                                    objectMapper.createObjectNode().put("text", prompt)
                                            ))
                            ))
            );

            // ✅ Step 5: Send request to Gemini
            String url = GEMINI_API_BASE + geminiModel + ":generateContent?key=" + geminiApiKey;

            RequestBody body = RequestBody.create(jsonRequest, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    System.err.println("❌ Gemini API Error: " + responseBody);
                    return "⚠️ Gemini API error: " + response.code() + " - " + response.message();
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode textNode = root.at("/candidates/0/content/parts/0/text");

                if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                    System.err.println("⚠️ Unexpected Gemini response: " + responseBody);
                    return "⚠️ Gemini gave an empty or unexpected response.";
                }

                String formatted = formatGeminiResponse(textNode.asText());
                return injectProductLinks(formatted, allProducts).trim();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "⚠️ Network or I/O error while contacting Gemini API.";
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Oops! Something went wrong while contacting Gemini API.";
        }
    }

    // 🧼 Step 6: Clean formatting
    private String formatGeminiResponse(String text) {
        if (text == null) return "";
        return text
                .replaceAll("\\*\\*", "")
                .replaceAll("\\*", "")
                .replaceAll("(?m)^\\s*[-•]\\s*", "• ")
                .replaceAll("\\n{2,}", "\n")
                .replaceAll("(?m)^\\s+", "")
                .trim();
    }

    // 🧠 Step 7: Add clickable links for product names
    private String injectProductLinks(String response, List<Product> allProducts) {
        if (response == null || response.isBlank()) return response;

        String result = response;
        for (Product p : allProducts) {
            if (p.getName() == null) continue;
            String name = Pattern.quote(p.getName());
            String regex = "(?i)" + name + "(?=\\s*\\(₹?\\d|\\s|$)";
            String replacement = String.format(
                    "<a href='%s%d' target='_blank' style='text-decoration:none;color:#007bff;'>%s</a>",
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
