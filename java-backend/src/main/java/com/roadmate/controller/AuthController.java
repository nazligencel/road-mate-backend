package com.roadmate.controller;

import com.roadmate.dto.auth.AuthResponse;
import com.roadmate.dto.auth.ForgotPasswordRequest;
import com.roadmate.dto.auth.GoogleLoginRequest;
import com.roadmate.dto.auth.LoginRequest;
import com.roadmate.dto.auth.RegisterRequest;
import com.roadmate.dto.auth.ResetPasswordRequest;
import com.roadmate.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/test-login")
    public ResponseEntity<AuthResponse> testLogin() {
        return ResponseEntity.ok(authService.testLogin());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Şifre sıfırlama kodu gönderildi"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Şifre başarıyla sıfırlandı"));
    }
}
