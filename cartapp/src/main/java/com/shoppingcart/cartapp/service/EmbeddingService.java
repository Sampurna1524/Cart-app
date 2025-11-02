package com.shoppingcart.cartapp.service;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // set this in application.properties or the environment
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    // Optional: custom base if you want to use a different host/region
    @Value("${gemini.api.base:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBase;

    // model to use (Gemini embedding stable)
    @Value("${gemini.embedding.model:gemini-embedding-001}")
    private String modelName;

    /**
     * Create embedding for a single text. Returns a double[] vector.
     * Throws RuntimeException on failure.
     */
    public double[] createEmbedding(String text) {
        if (text == null) text = "";

        // If no API key configured, fall back to a cheap deterministic embedding (not recommended for production)
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return quickHashEmbedding(text);
        }

        try {
            // REST endpoint pattern from Gemini docs:
            // POST {base}/models/{model}:embedContent
            String url = String.format("%s/models/%s:embedContent", geminiBase, modelName);

            // Build JSON payload:
            /*
              According to the docs, body example:
              {
                "model":"gemini-embedding-001",
                "content": { "parts": [ { "text": "..." } ] }
              }
            */
            JsonNode payload = mapper.createObjectNode()
                    .put("model", modelName)
                    .set("content", mapper.createObjectNode().set("parts",
                            mapper.createArrayNode().add(mapper.createObjectNode().put("text", text))));

            RequestBody body = RequestBody.create(mapper.writeValueAsString(payload), MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    // docs show x-goog-api-key header for API-key auth
                    .header("x-goog-api-key", geminiApiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    throw new RuntimeException("Gemini embed API returned " + response.code() + " : " + respBody);
                }
                String resp = response.body().string();
                JsonNode root = mapper.readTree(resp);
                // path per doc: result.embeddings or data[0].embedding format — support common shapes:
                // Check common shape: root.embeddings or root.data[0].embedding
                JsonNode embeddingNode = null;
                if (root.has("embeddings")) {
                    embeddingNode = root.get("embeddings").get(0);
                } else if (root.has("data") && root.get("data").isArray() && root.get("data").size() > 0) {
                    // some endpoints place the vector at data[0].embedding
                    JsonNode first = root.get("data").get(0);
                    if (first.has("embedding")) embeddingNode = first.get("embedding");
                    else if (first.has("embeddingVector")) embeddingNode = first.get("embeddingVector");
                } else if (root.has("embedding")) {
                    embeddingNode = root.get("embedding");
                }

                if (embeddingNode == null) {
                    // fallback: try to search for any numeric array inside JSON (defensive)
                    embeddingNode = findFirstNumericArray(root);
                }

                if (embeddingNode == null || !embeddingNode.isArray()) {
                    throw new RuntimeException("Could not parse embedding from Gemini response: " + resp);
                }

                List<Double> tmp = new ArrayList<>();
                for (JsonNode n : embeddingNode) tmp.add(n.asDouble());
                double[] vector = new double[tmp.size()];
                for (int i = 0; i < tmp.size(); i++) vector[i] = tmp.get(i);
                return vector;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini embedding API: " + e.getMessage(), e);
        }
    }

    // Helper: pretty-primitive deterministic embedding (fallback only)
    private double[] quickHashEmbedding(String text) {
        double[] v = new double[768]; // default size (approx)
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            v[i % v.length] += (bytes[i] & 0xFF) / 255.0;
        }
        // normalize
        double norm = 0;
        for (double x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm == 0) return v;
        for (int j = 0; j < v.length; j++) v[j] /= norm;
        return v;
    }

    // Helper to find any numeric array in a JSON tree (defensive)
    private JsonNode findFirstNumericArray(JsonNode node) {
        if (node == null) return null;
        if (node.isArray()) {
            boolean allNum = true;
            for (JsonNode c : node) if (!c.isNumber()) { allNum = false; break; }
            if (allNum) return node;
        }
        if (node.isObject()) {
            for (JsonNode child : node) {
                JsonNode found = findFirstNumericArray(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    // Convert vector to JSON string for DB storage
    public String toJson(double[] vector) {
        try {
            return mapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize vector", e);
        }
    }

    // Convert stored JSON string back to double[]
    public double[] fromJson(String json) {
        try {
            Double[] arr = mapper.readValue(json, Double[].class);
            double[] out = new double[arr.length];
            for (int i = 0; i < arr.length; i++) out[i] = arr[i];
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse vector JSON", e);
        }
    }
}
