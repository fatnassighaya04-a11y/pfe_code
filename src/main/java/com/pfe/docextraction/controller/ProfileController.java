package com.pfe.docextraction.controller;

import com.pfe.docextraction.dto.ProfileResponse;
import com.pfe.docextraction.dto.ProfileUpdateRequest;
import com.pfe.docextraction.service.auth.JwtService;
import com.pfe.docextraction.service.auth.ProfileService;
import com.pfe.docextraction.service.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;
    private final EmailService emailService;

    private UUID getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            return profileService.findByEmail(email).getId();
        }
        throw new RuntimeException("Utilisateur non authentifié");
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> getProfile(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        return ResponseEntity.ok(profileService.getCurrentUserProfile(userId));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            HttpServletRequest servletRequest) {
        UUID userId = getCurrentUserId(servletRequest);
        ProfileResponse updatedProfile = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(updatedProfile);
    }
}