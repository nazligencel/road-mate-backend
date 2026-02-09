package com.roadmate.controller;

import com.roadmate.dto.NomadDto;
import com.roadmate.model.User;
import com.roadmate.repository.BlockedUserRepository;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RoadMateController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/")
    public Map<String, String> getHome() {
        return Map.of("message", "RoadMate Java API is running", "version", "1.0.0");
    }

    @PostMapping("/update-location")
    public ResponseEntity<?> updateLocation(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {
        try {
            User user = null;

            // Try to get user from JWT token first
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtUtils.getEmailFromJwtToken(token);
                user = userRepository.findByEmail(email).orElse(null);
            }

            // Fallback to userId from payload if no token
            if (user == null && payload.get("userId") != null) {
                Long userId = Long.valueOf(payload.get("userId").toString());
                user = userRepository.findById(userId).orElse(null);
            }

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Double lat = Double.valueOf(payload.get("latitude").toString());
            Double lng = Double.valueOf(payload.get("longitude").toString());

            user.setLatitude(lat);
            user.setLongitude(lng);
            user.setLastActive(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("success", true, "userId", user.getId(), "timestamp", LocalDateTime.now()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/nearby-nomads")
    public List<NomadDto> getNearbyNomads(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Get current user to exclude from results and check subscription
        Long currentUserId = null;
        boolean isPro = false;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String email = jwtUtils.getEmailFromJwtToken(token);
                User currentUser = userRepository.findByEmail(email).orElse(null);
                if (currentUser != null) {
                    currentUserId = currentUser.getId();
                    isPro = "pro".equals(currentUser.getSubscriptionType());
                }
            } catch (Exception ignored) {}
        }

        List<User> users = userRepository.findNearbyNomads(lat, lng);
        final Long excludeId = currentUserId;
        final boolean requesterIsPro = isPro;

        // Get blocked user IDs to filter from results
        final List<Long> blockedIds = currentUserId != null
                ? blockedUserRepository.findBlockedUserIdsByBlockerId(currentUserId)
                : Collections.emptyList();

        return users.stream()
                .filter(user -> excludeId == null || !user.getId().equals(excludeId))
                .filter(user -> !blockedIds.contains(user.getId()))
                .map(user -> {
                    // Calculate distance using Haversine formula
                    double distance = calculateDistance(lat, lng, user.getLatitude(), user.getLongitude());

                    // Check if user is online (active within last 5 minutes)
                    boolean online = user.getLastActive() != null &&
                            ChronoUnit.MINUTES.between(user.getLastActive(), LocalDateTime.now()) < 5;

                    return NomadDto.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .image(user.getImage() != null ? user.getImage() : user.getProfileImageUrl())
                            .status(user.getStatus())
                            .vehicle(user.getVehicle())
                            .vehicleBrand(user.getVehicleBrand())
                            .vehicleModel(user.getVehicleModel())
                            .route(user.getRoute())
                            .latitude(user.getLatitude())
                            .longitude(user.getLongitude())
                            .distance(distance)
                            .online(online)
                            .sosActive(Boolean.TRUE.equals(user.getSosActive()))
                            .showRoute(requesterIsPro)
                            .coordinate(NomadDto.Coordinate.builder()
                                    .latitude(user.getLatitude())
                                    .longitude(user.getLongitude())
                                    .build())
                            .build();
                })
                // Free users: only see nomads within 50km
                .filter(nomad -> requesterIsPro || nomad.getDistance() <= 50.0)
                .collect(Collectors.toList());
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((R * c) * 10.0) / 10.0; // Round to 1 decimal
    }
}
