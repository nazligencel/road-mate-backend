package com.roadmate.controller;

import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RoadMateController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public Map<String, String> getHome() {
        return Map.of("message", "RoadMate Java API is running", "version", "1.0.0");
    }

    @PostMapping("/update-location")
    public ResponseEntity<?> updateLocation(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Double lat = Double.valueOf(payload.get("latitude").toString());
            Double lng = Double.valueOf(payload.get("longitude").toString());

            userRepository.findById(userId).ifPresentOrElse(user -> {
                user.setLatitude(lat);
                user.setLongitude(lng);
                user.setLastActive(LocalDateTime.now());
                userRepository.save(user);
            }, () -> {
                throw new RuntimeException("User not found");
            });

            return ResponseEntity.ok(Map.of("success", true, "timestamp", LocalDateTime.now()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/nearby-nomads")
    public List<User> getNearbyNomads(@RequestParam Double lat, @RequestParam Double lng) {
        return userRepository.findNearbyNomads(lat, lng);
    }
}
