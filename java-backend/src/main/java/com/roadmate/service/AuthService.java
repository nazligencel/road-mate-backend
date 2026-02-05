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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

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
}
