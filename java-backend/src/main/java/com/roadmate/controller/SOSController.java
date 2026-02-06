package com.roadmate.controller;

import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sos")
@CrossOrigin(origins = "*")
public class SOSController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Activate SOS - only for Pro users.
     * Marks the user as actively requesting roadside help.
     */
    @PostMapping("/activate")
    public ResponseEntity<?> activateSOS(
            @RequestHeader("Authorization") String authHeader) {
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

            user.setSosActive(true);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "sosActive", true,
                "message", "SOS activated. Nearby users will be notified."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Deactivate SOS.
     */
    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivateSOS(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            user.setSosActive(false);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "sosActive", false,
                "message", "SOS deactivated"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get nearby active SOS users - available to everyone.
     */
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbySOS(
            @RequestParam Double lat,
            @RequestParam Double lng) {
        try {
            List<User> allUsers = userRepository.findNearbyNomads(lat, lng);

            List<Map<String, Object>> sosUsers = allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.getSosActive()))
                .map(u -> {
                    double distance = calculateDistance(lat, lng, u.getLatitude(), u.getLongitude());
                    return Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getName() != null ? u.getName() : "",
                        "image", u.getImage() != null ? u.getImage() : (u.getProfileImageUrl() != null ? u.getProfileImageUrl() : ""),
                        "latitude", u.getLatitude(),
                        "longitude", u.getLongitude(),
                        "distance", distance,
                        "vehicle", u.getVehicle() != null ? u.getVehicle() : ""
                    );
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(sosUsers);
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

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((R * c) * 10.0) / 10.0;
    }
}
