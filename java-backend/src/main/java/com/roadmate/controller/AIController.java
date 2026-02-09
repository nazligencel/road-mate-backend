package com.roadmate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String SYSTEM_PROMPT = "You are RoadMate AI Assistant, a helpful road trip and vehicle companion. " +
        "You help nomads and travelers with: route planning, vehicle maintenance tips, " +
        "roadside troubleshooting, camping spot suggestions, weather advice, and general road trip guidance. " +
        "Keep responses concise and practical. Answer in the same language the user writes in.";

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            if (!"pro".equals(user.getSubscriptionType())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Pro subscription required",
                    "requiresPro", true
                ));
            }

            String message = payload.get("message").toString();

            String geminiKey = System.getProperty("GEMINI_API_KEY");
            if (geminiKey == null || geminiKey.isEmpty()) {
                geminiKey = System.getenv("GEMINI_API_KEY");
            }

            if (geminiKey == null || geminiKey.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of("error", "AI service not configured. Please set GEMINI_API_KEY."));
            }

            // Build request body using HashMap for reliability
            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", SYSTEM_PROMPT)));

            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", message)));

            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("maxOutputTokens", 500);
            genConfig.put("temperature", 0.7);

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("system_instruction", systemInstruction);
            requestBodyMap.put("contents", List.of(userContent));
            requestBodyMap.put("generationConfig", genConfig);

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);

            String url = GEMINI_API_URL + "?key=" + geminiKey;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                String detail = response.body();
                System.err.println("Gemini API error (" + response.statusCode() + "): " + detail);
                return ResponseEntity.status(502).body(Map.of("error", "AI service error", "detail", detail));
            }

            JsonNode root = objectMapper.readTree(response.body());
            String aiReply = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText();

            if (aiReply == null || aiReply.isEmpty()) {
                return ResponseEntity.ok(Map.of("reply", "Sorry, I couldn't process your request."));
            }

            return ResponseEntity.ok(Map.of("reply", aiReply));
        } catch (Exception e) {
            System.err.println("AI chat exception: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        String email = jwtUtils.getEmailFromJwtToken(token);
        return userRepository.findByEmail(email).orElse(null);
    }
}
