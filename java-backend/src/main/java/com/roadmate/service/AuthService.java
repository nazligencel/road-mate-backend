package com.roadmate.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.roadmate.dto.auth.AuthResponse;
import com.roadmate.dto.auth.GoogleLoginRequest;
import com.roadmate.dto.auth.LoginRequest;
import com.roadmate.dto.auth.RegisterRequest;
import com.roadmate.exception.BadRequestException;
import com.roadmate.exception.ConflictException;
import com.roadmate.exception.ResourceNotFoundException;
import com.roadmate.exception.UnauthorizedException;
import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.roadmate.dto.auth.ForgotPasswordRequest;
import com.roadmate.dto.auth.ResetPasswordRequest;
import com.roadmate.model.PasswordResetToken;
import com.roadmate.repository.PasswordResetTokenRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    EmailService emailService;

    @Value("${google.client.id}")
    private String googleClientId;

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

             GoogleIdToken idToken = verifier.verify(request.getToken());
             if (idToken != null) {
                 GoogleIdToken.Payload payload = idToken.getPayload();
                 String email = payload.getEmail();
                 String name = (String) payload.get("name");
                 String pictureUrl = (String) payload.get("picture");

                 Optional<User> userOptional = userRepository.findByEmail(email);
                 User user;
                 if (userOptional.isPresent()) {
                     user = userOptional.get();
                 } else {
                     user = User.builder()
                             .email(email)
                             .name(name)
                             .image(pictureUrl)
                             .provider("google")
                             .providerId(payload.getSubject())
                             .status("active")
                             .build();
                     userRepository.save(user);
                 }

                 String token = jwtUtils.generateToken(user.getEmail());
                 return new AuthResponse(token, user.getEmail(), user.getName());
             } else {
                 throw new UnauthorizedException("Geçersiz Google token");
             }
        } catch (GeneralSecurityException | IOException e) {
            throw new UnauthorizedException("Google token doğrulaması başarısız: " + e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
        return new AuthResponse(jwt, user.getEmail(), user.getName());
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Bu e-posta adresi zaten kullanımda");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("active")
                .provider("local")
                .build();

        userRepository.save(user);

        String jwt = jwtUtils.generateToken(user.getEmail());
        return new AuthResponse(jwt, user.getEmail(), user.getName());
    }

    public AuthResponse testLogin() {
        String testEmail = "test@roadmate.com";

        Optional<User> userOptional = userRepository.findByEmail(testEmail);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!"pro".equals(user.getSubscriptionType())) {
                user.setSubscriptionType("pro");
                userRepository.save(user);
            }
        } else {
            user = User.builder()
                    .email(testEmail)
                    .name("Test User")
                    .password(passwordEncoder.encode("test123"))
                    .status("active")
                    .provider("local")
                    .subscriptionType("pro")
                    .build();
            userRepository.save(user);
        }

        String jwt = jwtUtils.generateToken(user.getEmail());
        return new AuthResponse(jwt, user.getEmail(), user.getName());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Bu e-posta adresiyle kayıtlı kullanıcı bulunamadı"));

        if (!"local".equals(user.getProvider())) {
            throw new BadRequestException(
                    "Bu hesap " + user.getProvider() + " ile oluşturulmuş. Lütfen " + user.getProvider() + " ile giriş yapın.");
        }

        passwordResetTokenRepository.deleteByEmail(request.getEmail());

        String code = String.format("%06d", new Random().nextInt(999999));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(request.getEmail())
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetCode(request.getEmail(), code);
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByEmailAndCodeAndUsedFalse(request.getEmail(), request.getCode())
                .orElseThrow(() -> new BadRequestException("Geçersiz veya süresi dolmuş kod"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Kodun süresi dolmuş");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Mevcut şifre yanlış");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        userRepository.delete(user);
    }
}
