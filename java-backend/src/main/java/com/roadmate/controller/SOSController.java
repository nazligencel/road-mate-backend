package com.roadmate.controller;

import com.roadmate.model.User;
import com.roadmate.repository.BlockedUserRepository;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import com.roadmate.service.ExpoPushService;
import com.roadmate.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sos")
@CrossOrigin(origins = "*")
public class SOSController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ExpoPushService expoPushService;

    @Autowired
    private NotificationService notificationService;

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

            if (user.getLatitude() == null || user.getLongitude() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Location not available. Please update your location first."));
            }

            user.setSosActive(true);
            user.setSosActivatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Find nearby users within 100km who have push tokens
            List<User> allNearbyUsers = userRepository.findUsersWithPushTokenWithinRadius(
                    user.getLatitude(), user.getLongitude(), 100.0, user.getId());

            // Filter out users who have blocked the SOS sender or are blocked by them
            List<User> nearbyUsers = allNearbyUsers.stream()
                    .filter(nearby -> !blockedUserRepository.existsBlockBetween(user.getId(), nearby.getId()))
                    .collect(java.util.stream.Collectors.toList());

            // Create in-app notifications and collect push tokens
            List<String> pushTokens = new ArrayList<>();
            String userName = user.getName() != null ? user.getName() : "A nomad";

            for (User nearby : nearbyUsers) {
                double distance = calculateDistance(
                        user.getLatitude(), user.getLongitude(),
                        nearby.getLatitude(), nearby.getLongitude());

                String notifData = String.format(
                        "{\"type\": \"SOS\", \"sosUserId\": %d, \"lat\": %f, \"lng\": %f}",
                        user.getId(), user.getLatitude(), user.getLongitude());

                notificationService.createNotification(
                        nearby, user, "SOS_ALERT",
                        "\uD83D\uDEA8 SOS Alert Nearby!",
                        userName + " needs roadside help " + Math.round(distance) + "km away",
                        notifData);

                if (nearby.getExpoPushToken() != null) {
                    pushTokens.add(nearby.getExpoPushToken());
                }
            }

            // Send push notifications
            if (!pushTokens.isEmpty()) {
                Map<String, Object> pushData = new HashMap<>();
                pushData.put("type", "SOS");
                pushData.put("sosUserId", user.getId());
                pushData.put("lat", user.getLatitude());
                pushData.put("lng", user.getLongitude());

                expoPushService.sendBatchPushNotifications(
                        pushTokens,
                        "\uD83D\uDEA8 SOS Alert Nearby!",
                        userName + " needs roadside help nearby!",
                        pushData);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "sosActive", true,
                "sosActivatedAt", user.getSosActivatedAt().toString(),
                "notifiedCount", nearbyUsers.size(),
                "message", "SOS activated. " + nearbyUsers.size() + " nearby users have been notified."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivateSOS(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            user.setSosActive(false);
            user.setSosActivatedAt(null);
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

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbySOS(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            List<User> sosUsers = userRepository.findActiveSOSUsersNearby(lat, lng);

            // Filter out blocked users if authenticated
            User currentUser = getUserFromToken(authHeader);
            if (currentUser != null) {
                List<Long> blockedIds = blockedUserRepository.findBlockedUserIdsByBlockerId(currentUser.getId());
                sosUsers = sosUsers.stream()
                        .filter(u -> !blockedIds.contains(u.getId()))
                        .collect(java.util.stream.Collectors.toList());
            }

            List<Map<String, Object>> result = sosUsers.stream()
                .map(u -> {
                    double distance = calculateDistance(lat, lng, u.getLatitude(), u.getLongitude());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getName() != null ? u.getName() : "");
                    map.put("image", u.getImage() != null ? u.getImage() : (u.getProfileImageUrl() != null ? u.getProfileImageUrl() : ""));
                    map.put("latitude", u.getLatitude());
                    map.put("longitude", u.getLongitude());
                    map.put("distance", distance);
                    map.put("vehicle", u.getVehicle() != null ? u.getVehicle() : "");
                    map.put("sosActivatedAt", u.getSosActivatedAt() != null ? u.getSosActivatedAt().toString() : null);
                    return map;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
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
