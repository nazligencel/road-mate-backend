package com.roadmate.controller;

import com.roadmate.model.AssistMessage;
import com.roadmate.model.AssistRequest;
import com.roadmate.model.User;
import com.roadmate.repository.AssistMessageRepository;
import com.roadmate.repository.AssistRequestRepository;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assist")
@CrossOrigin(origins = "*")
public class AssistController {

    @Autowired
    private AssistRequestRepository assistRequestRepository;

    @Autowired
    private AssistMessageRepository assistMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Create a new assist request - available to everyone.
     */
    @PostMapping
    public ResponseEntity<?> createAssistRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            AssistRequest request = AssistRequest.builder()
                .user(user)
                .title(payload.get("title").toString())
                .description(payload.get("description") != null ? payload.get("description").toString() : "")
                .latitude(payload.get("latitude") != null ? Double.valueOf(payload.get("latitude").toString()) : user.getLatitude())
                .longitude(payload.get("longitude") != null ? Double.valueOf(payload.get("longitude").toString()) : user.getLongitude())
                .build();

            AssistRequest saved = assistRequestRepository.save(request);
            return ResponseEntity.ok(mapRequestToDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List assist requests - available to everyone.
     * Optional query param: status (open/resolved)
     */
    @GetMapping
    public ResponseEntity<?> listAssistRequests(
            @RequestParam(required = false) String status) {
        try {
            List<AssistRequest> requests;
            if (status != null && !status.isEmpty()) {
                requests = assistRequestRepository.findByStatus(status);
            } else {
                requests = assistRequestRepository.findAllRecent();
            }

            List<Map<String, Object>> result = requests.stream()
                .map(this::mapRequestToDto)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get assist request detail with messages.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAssistDetail(@PathVariable Long id) {
        try {
            AssistRequest request = assistRequestRepository.findById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Assist request not found"));
            }

            List<AssistMessage> messages = assistMessageRepository.findByAssistRequestIdOrderByCreatedAtAsc(id);

            Map<String, Object> dto = mapRequestToDto(request);
            dto.put("messages", messages.stream().map(m -> {
                Map<String, Object> mDto = new LinkedHashMap<>();
                mDto.put("id", m.getId());
                mDto.put("content", m.getContent());
                mDto.put("userId", m.getUser().getId());
                mDto.put("userName", m.getUser().getName());
                mDto.put("userImage", getUserImage(m.getUser()));
                mDto.put("createdAt", m.getCreatedAt().toString());
                return mDto;
            }).collect(Collectors.toList()));

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add a message/solution to an assist request.
     */
    @PostMapping("/{id}/message")
    public ResponseEntity<?> addMessage(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            AssistRequest request = assistRequestRepository.findById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Assist request not found"));
            }

            AssistMessage message = AssistMessage.builder()
                .assistRequest(request)
                .user(user)
                .content(payload.get("content").toString())
                .build();

            AssistMessage saved = assistMessageRepository.save(message);

            Map<String, Object> mDto = new LinkedHashMap<>();
            mDto.put("id", saved.getId());
            mDto.put("content", saved.getContent());
            mDto.put("userId", user.getId());
            mDto.put("userName", user.getName());
            mDto.put("userImage", getUserImage(user));
            mDto.put("createdAt", saved.getCreatedAt().toString());

            return ResponseEntity.ok(mDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Edit an assist request - only the creator can edit.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> editAssistRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            AssistRequest request = assistRequestRepository.findById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Assist request not found"));
            }

            if (!request.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Only the creator can edit this request"));
            }

            if (payload.containsKey("title") && payload.get("title") != null) {
                request.setTitle(payload.get("title").toString());
            }
            if (payload.containsKey("description")) {
                request.setDescription(payload.get("description") != null ? payload.get("description").toString() : "");
            }

            assistRequestRepository.save(request);
            return ResponseEntity.ok(mapRequestToDto(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete an assist request - only the creator can delete.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAssistRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            AssistRequest request = assistRequestRepository.findById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Assist request not found"));
            }

            if (!request.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Only the creator can delete this request"));
            }

            assistMessageRepository.deleteAll(
                assistMessageRepository.findByAssistRequestIdOrderByCreatedAtAsc(id)
            );
            assistRequestRepository.delete(request);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mark an assist request as resolved.
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAssistRequest(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            AssistRequest request = assistRequestRepository.findById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Assist request not found"));
            }

            if (!request.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Only the creator can resolve this request"));
            }

            request.setStatus("resolved");
            assistRequestRepository.save(request);

            return ResponseEntity.ok(Map.of("success", true, "status", "resolved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> mapRequestToDto(AssistRequest request) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", request.getId());
        dto.put("title", request.getTitle());
        dto.put("description", request.getDescription());
        dto.put("latitude", request.getLatitude());
        dto.put("longitude", request.getLongitude());
        dto.put("status", request.getStatus());
        dto.put("createdAt", request.getCreatedAt().toString());
        dto.put("userId", request.getUser().getId());
        dto.put("userName", request.getUser().getName());
        dto.put("userImage", getUserImage(request.getUser()));
        long messageCount = assistMessageRepository.countByAssistRequestId(request.getId());
        dto.put("messageCount", messageCount);
        return dto;
    }

    private String getUserImage(User user) {
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            return user.getProfileImageUrl();
        }
        return user.getImage();
    }

    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        String email = jwtUtils.getEmailFromJwtToken(token);
        return userRepository.findByEmail(email).orElse(null);
    }
}
