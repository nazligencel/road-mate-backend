package com.roadmate.controller;

import com.roadmate.dto.CreateCommentRequest;
import com.roadmate.dto.CreateDiscussionRequest;
import com.roadmate.dto.DiscussionCommentDto;
import com.roadmate.dto.DiscussionDto;
import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import com.roadmate.service.DiscussionService;
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
@RequestMapping("/api/discussions")
@CrossOrigin(origins = "*")
public class DiscussionController {

    @Autowired
    private DiscussionService discussionService;

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

    // Get all discussions
    @GetMapping
    public ResponseEntity<List<DiscussionDto>> getDiscussions(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        List<DiscussionDto> dtos = discussionService.getDiscussions(currentUser);
        return ResponseEntity.ok(dtos);
    }

    // Get single discussion
    @GetMapping("/{id}")
    public ResponseEntity<DiscussionDto> getDiscussion(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        DiscussionDto dto = discussionService.getDiscussion(id, currentUser);
        return ResponseEntity.ok(dto);
    }

    // Create discussion
    @PostMapping
    public ResponseEntity<DiscussionDto> createDiscussion(
            @Valid @RequestBody CreateDiscussionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        User creator = getCurrentUser(authHeader);
        DiscussionDto dto = discussionService.createDiscussion(creator, request);
        return ResponseEntity.ok(dto);
    }

    // Upload discussion image
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

    // Get comments for a discussion
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<DiscussionCommentDto>> getComments(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        getCurrentUser(authHeader); // Verify auth
        List<DiscussionCommentDto> comments = discussionService.getComments(id);
        return ResponseEntity.ok(comments);
    }

    // Add comment to a discussion
    @PostMapping("/{id}/comments")
    public ResponseEntity<DiscussionCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader("Authorization") String authHeader) {
        User author = getCurrentUser(authHeader);
        DiscussionCommentDto comment = discussionService.addComment(author, id, request.getText());
        return ResponseEntity.ok(comment);
    }

    // Toggle bookmark
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        User user = getCurrentUser(authHeader);
        boolean isSaved = discussionService.toggleBookmark(user, id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "isSaved", isSaved
        ));
    }

    // Get saved discussions
    @GetMapping("/saved")
    public ResponseEntity<List<DiscussionDto>> getSavedDiscussions(
            @RequestHeader("Authorization") String authHeader) {
        User user = getCurrentUser(authHeader);
        List<DiscussionDto> dtos = discussionService.getSavedDiscussions(user);
        return ResponseEntity.ok(dtos);
    }
}
