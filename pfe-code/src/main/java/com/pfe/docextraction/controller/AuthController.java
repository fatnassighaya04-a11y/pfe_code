package com.pfe.docextraction.controller;

import com.pfe.docextraction.dto.AuthRequest;
import com.pfe.docextraction.dto.AuthResponse;
import com.pfe.docextraction.dto.RegisterRequest;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.repository.UserRepository;
import com.pfe.docextraction.service.audit.AuditService;
import com.pfe.docextraction.service.auth.AuthService;
import com.pfe.docextraction.service.email.EmailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request);
        auditService.log(null, "USER_REGISTER", "USER",
                request.getEmail(), "SUCCESS",
                httpRequest.getRemoteAddr(),
                "Nouvel utilisateur : " + request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request);
        auditService.log(null, "USER_LOGIN", "USER",
                request.getEmail(), "SUCCESS",
                httpRequest.getRemoteAddr(),
                "Connexion : " + request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        auditService.logAction(user, "USER_LOGOUT", "USER", user.getId().toString());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Déconnexion réussie");
        response.put("email", user.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("accountStatus", user.getAccountStatus());
        return ResponseEntity.ok(response);
    }

    // ÉTAPE 1 — Demander un code de réinitialisation (email normalisé)
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email").toLowerCase().trim();
        Map<String, Object> response = new HashMap<>();

        userRepository.findByEmail(email).ifPresent(user -> {
            String code = String.format("%06d", new Random().nextInt(999999));
            user.setResetCode(code);
            user.setResetCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            userRepository.saveAndFlush(user);
            emailService.sendResetCode(email, code);
            log.info("🔐 Code généré pour {} : {}", email, code);
        });

        response.put("message", "Si cet email existe, un code a été envoyé");
        return ResponseEntity.ok(response);
    }

    // ÉTAPE 2 — Vérifier le code et changer le mot de passe (email normalisé)
    @Transactional
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email").toLowerCase().trim();
        String code = body.get("code").trim();
        String newPassword = body.get("newPassword");

        log.info("➡️ Reset - email: {}, code reçu: {}, nouveau mot de passe: '{}'", email, code, newPassword);

        Map<String, Object> response = new HashMap<>();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        log.info("📦 Utilisateur trouvé: {}, resetCode stocké: {}", user.getEmail(), user.getResetCode());

        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            throw new RuntimeException("Code incorrect");
        }
        if (LocalDateTime.now().isAfter(user.getResetCodeExpiresAt())) {
            throw new RuntimeException("Code expiré — demandez un nouveau code");
        }

        String encoded = passwordEncoder.encode(newPassword);
        log.info("🔐 Nouveau hash généré: {}", encoded);

        user.setPasswordHash(encoded);
        user.setResetCode(null);
        user.setResetCodeExpiresAt(null);

        // Sauvegarde forcée avec flush
        User saved = userRepository.saveAndFlush(user);
        entityManager.refresh(saved); // pour être certain de lire depuis la base

        log.info("💾 Après sauvegarde - hash en base: {}", saved.getPasswordHash());

        response.put("message", "Mot de passe changé avec succès");
        return ResponseEntity.ok(response);
    }
}