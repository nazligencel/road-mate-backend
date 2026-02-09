package com.roadmate.controller;

import com.roadmate.dto.ActivityDto;
import com.roadmate.dto.CreateActivityRequest;
import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import com.roadmate.service.ActivityService;
import com.roadmate.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "*")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User getCurrentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization required");
        }
        String token = authHeader.substring(7);
        String email = jwtUtils.getEmailFromJwtToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Get activities (from connected users + own)
    @GetMapping
    public ResponseEntity<List<ActivityDto>> getActivities(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        List<ActivityDto> dtos = activityService.getActivities(currentUser);
        return ResponseEntity.ok(dtos);
    }

    // Upload activity image
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        getCurrentUser(authHeader); // Verify auth
        
        String fileName = fileStorageService.store(file);
        
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();

        return ResponseEntity.ok(Map.of("imageUrl", fileDownloadUri));
    }

    // Create activity
    @PostMapping
    public ResponseEntity<ActivityDto> createActivity(
            @Valid @RequestBody CreateActivityRequest request,
            @RequestHeader("Authorization") String authHeader) {
        User creator = getCurrentUser(authHeader);
        ActivityDto dto = activityService.createActivity(creator, request);
        return ResponseEntity.ok(dto);
    }

    // Join activity
    @PostMapping("/{activityId}/join")
    public ResponseEntity<Map<String, Object>> joinActivity(
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);

        try {
            activityService.joinActivity(currentUser, activityId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully joined the activity"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // Leave activity
    @PostMapping("/{activityId}/leave")
    public ResponseEntity<Map<String, Object>> leaveActivity(
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);

        try {
            activityService.leaveActivity(currentUser, activityId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully left the activity"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // Get single activity
    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityDto> getActivity(
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        ActivityDto dto = activityService.getActivity(activityId, currentUser);
        return ResponseEntity.ok(dto);
    }

    // Cancel activity
    @PutMapping("/{activityId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelActivity(
            @PathVariable Long activityId,
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        try {
            activityService.cancelActivity(currentUser, activityId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Activity cancelled successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
