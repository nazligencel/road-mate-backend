package com.roadmate.controller;

import com.roadmate.model.BlockedUser;
import com.roadmate.model.User;
import com.roadmate.repository.BlockedUserRepository;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blocks")
@CrossOrigin(origins = "*")
public class BlockController {

    @Autowired
    private BlockedUserRepository blockedUserRepository;

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

    // Block a user
    @PostMapping("/{userId}")
    public ResponseEntity<?> blockUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User currentUser = getCurrentUser(authHeader);

            if (currentUser.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You cannot block yourself"));
            }

            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (blockedUserRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is already blocked"));
            }

            BlockedUser block = BlockedUser.builder()
                    .blocker(currentUser)
                    .blocked(targetUser)
                    .createdAt(LocalDateTime.now())
                    .build();

            blockedUserRepository.save(block);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User blocked successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Unblock a user
    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<?> unblockUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User currentUser = getCurrentUser(authHeader);

            if (!blockedUserRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not blocked"));
            }

            blockedUserRepository.deleteByBlockerIdAndBlockedId(currentUser.getId(), userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User unblocked successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get blocked users list
    @GetMapping
    public ResponseEntity<?> getBlockedUsers(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User currentUser = getCurrentUser(authHeader);

            List<BlockedUser> blockedUsers = blockedUserRepository.findByBlockerId(currentUser.getId());

            List<Map<String, Object>> result = blockedUsers.stream().map(b -> {
                User blocked = b.getBlocked();
                return Map.<String, Object>of(
                        "id", blocked.getId(),
                        "name", blocked.getName() != null ? blocked.getName() : "",
                        "image", blocked.getProfileImageUrl() != null && !blocked.getProfileImageUrl().isEmpty() ? blocked.getProfileImageUrl() :
                                (blocked.getImage() != null ? blocked.getImage() : ""),
                        "blockedAt", b.getCreatedAt().toString()
                );
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Check if a specific user is blocked
    @GetMapping("/check/{userId}")
    public ResponseEntity<?> checkBlocked(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User currentUser = getCurrentUser(authHeader);

            boolean isBlocked = blockedUserRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), userId);
            boolean isBlockedByThem = blockedUserRepository.existsByBlockerIdAndBlockedId(userId, currentUser.getId());

            return ResponseEntity.ok(Map.of(
                    "isBlocked", isBlocked,
                    "isBlockedByThem", isBlockedByThem
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
