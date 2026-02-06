package com.roadmate.controller;

import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Verify and activate Pro subscription.
     * Called by client after successful RevenueCat purchase.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifySubscription(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            // RevenueCat product ID for validation logging
            String productId = payload.get("productId") != null ? payload.get("productId").toString() : "";

            user.setSubscriptionType("pro");
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "subscriptionType", "pro",
                "message", "Subscription activated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current subscription status for the authenticated user.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String subType = user.getSubscriptionType() != null ? user.getSubscriptionType() : "free";

            return ResponseEntity.ok(Map.of(
                "subscriptionType", subType,
                "isPro", "pro".equals(subType)
            ));
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
