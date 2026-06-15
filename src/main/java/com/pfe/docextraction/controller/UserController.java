package com.pfe.docextraction.controller;

import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.enums.AccountStatus;
import com.pfe.docextraction.enums.UserRole;
import com.pfe.docextraction.repository.UserRepository;
import com.pfe.docextraction.service.audit.AuditService;
import com.pfe.docextraction.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream()
            .map(this::toMap)
            .toList();
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getPendingUsers() {
        List<User> users = userRepository.findAll().stream()
            .filter(u -> u.getAccountStatus() == AccountStatus.PENDING)
            .toList();
        List<Map<String, Object>> response = users.stream()
            .map(this::toMap)
            .toList();
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countPendingUsers() {
        long count = userRepository.countByAccountStatus(AccountStatus.PENDING);
        return ResponseEntity.ok(count);
    }

    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin,
            HttpServletRequest httpRequest) {
        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Log role before change and keep role unchanged
        log.info("Approving user {} (email={}) - current role={}", user.getId(), user.getEmail(), user.getRole());

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        emailService.sendAccountApprovedNotification(user.getUsername(), user.getEmail());
        auditService.logAction(admin, "USER_APPROVE", "USER", user.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte approuvé avec succès");
        response.put("email", user.getEmail());
        response.put("accountStatus", user.getAccountStatus());
        response.put("role", user.getRole());
        log.info("After approval user {} role={}", user.getId(), user.getRole());
        return ResponseEntity.ok(response);
    }

    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> rejectUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin,
            HttpServletRequest httpRequest) {
        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setAccountStatus(AccountStatus.REJECTED);
        userRepository.save(user);

        emailService.sendAccountRejectedNotification(user.getUsername(), user.getEmail());
        auditService.logAction(admin, "USER_REJECT", "USER", user.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte rejeté");
        response.put("email", user.getEmail());
        response.put("accountStatus", user.getAccountStatus());
        return ResponseEntity.ok(response);
    }

    
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, Object>> activateUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin) {
        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        boolean wasActive = Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(true);
        userRepository.save(user);
        if (!wasActive) {
            emailService.sendAccountModificationNotification(
                user.getEmail(),
                "Votre compte a été activé",
                "Votre compte a été activé par l'administrateur. Vous pouvez maintenant accéder à la plateforme."
            );
        }
        auditService.logAction(admin, "USER_ACTIVATE", "USER", user.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte activé");
        response.put("isActive", user.getIsActive());
        return ResponseEntity.ok(response);
    }

    
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, Object>> deactivateUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin) {
        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        boolean wasActive = Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(false);
        userRepository.save(user);
        if (wasActive) {
            emailService.sendAccountModificationNotification(
                user.getEmail(),
                "Votre compte a été désactivé",
                "Votre compte a été désactivé par l'administrateur. Si vous pensez qu'il s'agit d'une erreur, contactez l'administrateur."
            );
        }
        auditService.logAction(admin, "USER_DEACTIVATE", "USER", user.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte désactivé");
        response.put("isActive", user.getIsActive());
        return ResponseEntity.ok(response);
    }

    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserRole previousRole = user.getRole();
        AccountStatus previousStatus = user.getAccountStatus();
        Boolean previousActive = user.getIsActive();
        Boolean previousLocked = user.getIsLocked();

        if (body.containsKey("role")) {
            user.setRole(UserRole.valueOf(body.get("role")));
        }
        if (body.containsKey("accountStatus")) {
            user.setAccountStatus(AccountStatus.valueOf(body.get("accountStatus")));
        }
        if (body.containsKey("isActive")) {
            user.setIsActive(Boolean.valueOf(body.get("isActive")));
        }
        if (body.containsKey("isLocked")) {
            user.setIsLocked(Boolean.valueOf(body.get("isLocked")));
        }
        userRepository.save(user);

        boolean changed = false;
        if (previousRole != user.getRole()) {
            
            changed = true;
        }
        if (previousStatus != user.getAccountStatus()) {
            changed = true;
        }
        if (previousActive != null && !previousActive.equals(user.getIsActive())) {
            
            changed = true;
        }
        if (previousLocked != null && !previousLocked.equals(user.getIsLocked())) {
            changed = true;
        }
        if (changed) {
            StringBuilder details = new StringBuilder("Votre compte a été mis à jour par l'administrateur.");
            if (previousRole != user.getRole()) {
                details.append("\n- Rôle : ").append(previousRole).append(" → ").append(user.getRole());
                auditService.logAction(admin, "USER_ROLE_CHANGE", "USER", user.getId().toString());
            }
            if (previousStatus != user.getAccountStatus()) {
                details.append("\n- Statut du compte : ").append(previousStatus).append(" → ").append(user.getAccountStatus());
                auditService.logAction(admin, "USER_STATUS_CHANGE", "USER", user.getId().toString());
            }
            if (previousActive != null && !previousActive.equals(user.getIsActive())) {
                details.append("\n- Activé : ").append(previousActive).append(" → ").append(user.getIsActive());
                auditService.logAction(admin,
                        Boolean.TRUE.equals(user.getIsActive()) ? "USER_ACTIVATE" : "USER_DEACTIVATE",
                        "USER",
                        user.getId().toString());
            }
            if (previousLocked != null && !previousLocked.equals(user.getIsLocked())) {
                details.append("\n- Verrouillé : ").append(previousLocked).append(" → ").append(user.getIsLocked());
                auditService.logAction(admin,
                        Boolean.TRUE.equals(user.getIsLocked()) ? "USER_LOCK" : "USER_UNLOCK",
                        "USER",
                        user.getId().toString());
            }
            emailService.sendAccountModificationNotification(
                user.getEmail(),
                "Votre compte a été modifié",
                details.toString()
            );
        } else {
            auditService.logAction(admin, "USER_UPDATE", "USER", user.getId().toString());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Utilisateur mis à jour");
        response.put("role", user.getRole());
        response.put("accountStatus", user.getAccountStatus());
        return ResponseEntity.ok(response);
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin) {
        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        emailService.sendAccountModificationNotification(
            user.getEmail(),
            "Votre compte a été supprimé",
            "Votre compte a été supprimé par l'administrateur. Si vous pensez qu'il s'agit d'une erreur, contactez l'administrateur."
        );
        auditService.logAction(admin, "USER_DELETE", "USER", user.getId().toString());
        userRepository.delete(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Utilisateur supprimé");
        return ResponseEntity.ok(response);
    }

    
    private Map<String, Object> toMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("role", user.getRole());
        map.put("accountStatus", user.getAccountStatus());
        map.put("isActive", user.getIsActive());
        map.put("isLocked", user.getIsLocked());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    
    @PutMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, Object>> lockUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin) {
        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        boolean wasLocked = Boolean.TRUE.equals(user.getIsLocked());
        user.setIsLocked(true);
        userRepository.save(user);
        if (!wasLocked) {
            emailService.sendAccountModificationNotification(
                user.getEmail(),
                "Votre compte a été bloqué",
                "Votre compte a été bloqué par l'administrateur. Vous ne pouvez plus vous connecter tant qu'il n'est pas débloqué."
            );
        }
        auditService.logAction(admin, "USER_LOCK", "USER", user.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte bloqué");
        response.put("isLocked", user.getIsLocked());
        return ResponseEntity.ok(response);
    }

    
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, Object>> unlockUser(
            @PathVariable String id,
            @AuthenticationPrincipal User admin) {
        User user = userRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        boolean wasLocked = Boolean.TRUE.equals(user.getIsLocked());
        user.setIsLocked(false);
        
        user.setFailedAttempts(0);
        userRepository.save(user);
        if (wasLocked) {
            emailService.sendAccountModificationNotification(
                user.getEmail(),
                "Votre compte a été débloqué",
                "Votre compte a été débloqué par l'administrateur. Vous pouvez à nouveau vous connecter à la plateforme."
            );
        }
        auditService.logAction(admin, "USER_UNLOCK", "USER", user.getId().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte débloqué");
        response.put("isLocked", user.getIsLocked());
        return ResponseEntity.ok(response);
    }
}