package com.roadmate.controller;

import com.roadmate.model.GalleryPhoto;
import com.roadmate.model.User;
import com.roadmate.repository.GalleryPhotoRepository;
import com.roadmate.repository.UserRepository;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GalleryPhotoRepository galleryPhotoRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
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
}
