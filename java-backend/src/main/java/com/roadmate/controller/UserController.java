package com.roadmate.controller;

import com.roadmate.dto.ChangePasswordRequest;
import com.roadmate.model.GalleryPhoto;
import com.roadmate.model.User;
import com.roadmate.repository.GalleryPhotoRepository;
import com.roadmate.repository.UserRepository;
import com.roadmate.service.AuthService;
import com.roadmate.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

import com.roadmate.dto.UserProfileUpdateDto;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GalleryPhotoRepository galleryPhotoRepository;

    @Autowired
    private AuthService authService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        User user = getCurrentUser();
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserProfileUpdateDto updateRequest) {
        User user = getCurrentUser();
        
        if (updateRequest.getName() != null) {
            user.setName(updateRequest.getName());
        }
        if (updateRequest.getVehicle() != null) {
            user.setVehicle(updateRequest.getVehicle());
        }
        if (updateRequest.getStatus() != null) {
            user.setStatus(updateRequest.getStatus());
        }

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/profile-image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        User user = getCurrentUser();
        String fileName = fileStorageService.store(file);
        
        String fileDownloadUri = "/uploads/" + fileName;

        user.setProfileImageUrl(fileDownloadUri);
        // Also update the 'image' field if it's currently used for display
        user.setImage(fileDownloadUri); 
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile image updated successfully", "url", fileDownloadUri));
    }

    @PostMapping("/gallery")
    public ResponseEntity<?> uploadGalleryPhoto(@RequestParam("file") MultipartFile file) {
        User user = getCurrentUser();
        String fileName = fileStorageService.store(file);

        String fileDownloadUri = "/uploads/" + fileName;

        GalleryPhoto photo = GalleryPhoto.builder()
                .user(user)
                .photoUrl(fileDownloadUri)
                .build();
        
        galleryPhotoRepository.save(photo);

        return ResponseEntity.ok(photo);
    }

    @GetMapping("/gallery")
    public ResponseEntity<List<GalleryPhoto>> getGalleryPhotos() {
        User user = getCurrentUser();
        List<GalleryPhoto> photos = galleryPhotoRepository.findByUserId(user.getId());
        return ResponseEntity.ok(photos);
    }

    @DeleteMapping("/gallery/{photoId}")
    public ResponseEntity<?> deleteGalleryPhoto(@PathVariable Long photoId) {
        User user = getCurrentUser();
        GalleryPhoto photo = galleryPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Not authorized to delete this photo");
        }

        // Extract filename from URL (assuming stored as /uploads/filename)
        String photoUrl = photo.getPhotoUrl();
        String filename = photoUrl.substring(photoUrl.lastIndexOf("/") + 1);

        try {
            fileStorageService.delete(filename);
        } catch (Exception e) {
            // Log error but continue to delete from DB? 
            // Better to fail or just log. For now, we proceed to ensure DB consistency or throw.
            // Let's assume if file delete fails, we might still want to remove the record 
            // or we might want to throw. Given it's a "clean up", let's proceed with DB delete.
            System.err.println("File deletion failed: " + e.getMessage());
        }

        galleryPhotoRepository.delete(photo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        User user = getCurrentUser();
        authService.changePassword(user.getEmail(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Şifre başarıyla değiştirildi"));
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount() {
        User user = getCurrentUser();
        authService.deleteAccount(user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Hesap başarıyla silindi"));
    }
}
