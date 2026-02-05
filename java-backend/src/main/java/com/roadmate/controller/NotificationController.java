package com.roadmate.controller;

import com.roadmate.dto.NotificationDto;
import com.roadmate.model.Notification;
import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import com.roadmate.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

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

    // Get all notifications
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);

        List<Notification> notifications = notificationService.getNotifications(currentUser.getId());

        List<NotificationDto> dtos = notifications.stream().map(this::mapToDto).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Get unread notifications
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);

        List<Notification> notifications = notificationService.getUnreadNotifications(currentUser.getId());

        List<NotificationDto> dtos = notifications.stream().map(this::mapToDto).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Get unread count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        Long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Mark all as read
    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // Mark single notification as read
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("Authorization") String authHeader) {
        getCurrentUser(authHeader); // Verify user is authenticated
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    // Send meeting request (Create Meeting Point)
    @PostMapping("/meeting-request")
    public ResponseEntity<Map<String, Object>> sendMeetingRequest(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        User sender = getCurrentUser(authHeader);
        Long targetUserId = request.get("targetUserId");

        Notification savedNotification = notificationService.sendMeetingRequest(sender, targetUserId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Meeting request sent!",
                "notificationId", savedNotification.getId()
        ));
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .senderId(n.getSender() != null ? n.getSender().getId() : null)
                .senderName(n.getSender() != null ? n.getSender().getName() : null)
                .senderImage(n.getSender() != null ? n.getSender().getImage() : null)
                .data(n.getData())
                .build();
    }
}
