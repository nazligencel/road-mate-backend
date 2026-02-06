package com.roadmate.controller;

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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * AI Chat - Pro only.
     * Proxies to OpenAI GPT for road/vehicle assistance.
     */
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

            String openaiKey = System.getenv("OPENAI_API_KEY");
            if (openaiKey == null || openaiKey.isEmpty()) {
                // Try dotenv
                try {
                    io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
                    openaiKey = dotenv.get("OPENAI_API_KEY");
                } catch (Exception ignored) {}
            }

            if (openaiKey == null || openaiKey.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of("error", "AI service not configured. Please set OPENAI_API_KEY."));
            }

            String systemPrompt = "You are RoadMate AI Assistant, a helpful road trip and vehicle companion. " +
                "You help nomads and travelers with: route planning, vehicle maintenance tips, " +
                "roadside troubleshooting, camping spot suggestions, weather advice, and general road trip guidance. " +
                "Keep responses concise and practical. Answer in the same language the user writes in.";

            String requestBody = """
                {
                    "model": "gpt-4o-mini",
                    "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                    ],
                    "max_tokens": 500,
                    "temperature": 0.7
                }
                """.formatted(
                    systemPrompt.replace("\"", "\\\""),
                    message.replace("\"", "\\\"").replace("\n", "\\n")
                );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity.status(502).body(Map.of("error", "AI service error", "detail", response.body()));
            }

            // Parse the response to extract the message content
            String responseBody = response.body();
            // Simple JSON extraction for the content field
            int contentStart = responseBody.indexOf("\"content\":");
            if (contentStart == -1) {
                return ResponseEntity.ok(Map.of("reply", "Sorry, I couldn't process your request."));
            }

            // Find the content value - it's after "content": "
            int valueStart = responseBody.indexOf("\"", contentStart + 10) + 1;
            int valueEnd = responseBody.indexOf("\"", valueStart);
            // Handle escaped quotes
            while (valueEnd > 0 && responseBody.charAt(valueEnd - 1) == '\\') {
                valueEnd = responseBody.indexOf("\"", valueEnd + 1);
            }

            String aiReply = responseBody.substring(valueStart, valueEnd)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");

            return ResponseEntity.ok(Map.of("reply", aiReply));
        } catch (Exception e) {
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
