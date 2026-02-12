package com.roadmate.controller;

import com.roadmate.dto.ConnectionDTO;
import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;
    private final UserRepository userRepository;

    /**
     * QR tarandığında bağlantı isteği gönder
     * POST /api/connections/scan/{targetUserId}
     */
    @PostMapping("/scan/{targetUserId}")
    public ResponseEntity<?> connectByQR(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromDetails(userDetails);
            ConnectionDTO connection = connectionService.connectByQR(userId, targetUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bağlantı isteği gönderildi");
            response.put("connection", connection);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Kullanıcının bağlantılarını getir
     * GET /api/connections/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<ConnectionDTO>> getMyConnections(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromDetails(userDetails);
        List<ConnectionDTO> connections = connectionService.getMyConnections(userId);
        return ResponseEntity.ok(connections);
    }

    /**
     * Bekleyen bağlantı isteklerini getir
     * GET /api/connections/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ConnectionDTO>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromDetails(userDetails);
        List<ConnectionDTO> pending = connectionService.getPendingRequests(userId);
        return ResponseEntity.ok(pending);
    }

    /**
     * Bağlantı sayısını getir
     * GET /api/connections/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getConnectionCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromDetails(userDetails);
        Long count = connectionService.getConnectionCount(userId);
        
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Bağlantı isteğini kabul et
     * PUT /api/connections/{id}/accept
     */
    @PutMapping("/{connectionId}/accept")
    public ResponseEntity<?> acceptConnection(
            @PathVariable Long connectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromDetails(userDetails);
            ConnectionDTO connection = connectionService.acceptConnection(connectionId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bağlantı kabul edildi");
            response.put("connection", connection);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Bağlantı isteğini reddet
     * PUT /api/connections/{id}/reject
     */
    @PutMapping("/{connectionId}/reject")
    public ResponseEntity<?> rejectConnection(
            @PathVariable Long connectionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromDetails(userDetails);
            ConnectionDTO connection = connectionService.rejectConnection(connectionId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bağlantı reddedildi");
            response.put("connection", connection);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Profil/explore'dan arkadaş isteği gönder
     * POST /api/connections/request/{targetUserId}
     */
    @PostMapping("/request/{targetUserId}")
    public ResponseEntity<?> sendConnectionRequest(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromDetails(userDetails);
            ConnectionDTO connection = connectionService.sendConnectionRequest(userId, targetUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Friend request sent");
            response.put("connection", connection);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * İki kullanıcı arası bağlantı durumu kontrolü
     * GET /api/connections/status/{userId}
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, String>> getConnectionStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = getUserIdFromDetails(userDetails);
        String status = connectionService.getConnectionStatus(currentUserId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    /**
     * Arkadaşı sil / isteği iptal et
     * DELETE /api/connections/user/{userId}
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> removeConnection(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long currentUserId = getUserIdFromDetails(userDetails);
            connectionService.removeConnection(currentUserId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Connection removed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * UserDetails'dan userId'yi al
     */
    private Long getUserIdFromDetails(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        return user.getId();
    }
}
