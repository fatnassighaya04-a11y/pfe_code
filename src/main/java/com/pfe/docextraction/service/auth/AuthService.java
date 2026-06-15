package com.pfe.docextraction.service.auth;

import com.pfe.docextraction.dto.AuthRequest;
import com.pfe.docextraction.dto.AuthResponse;
import com.pfe.docextraction.dto.RegisterRequest;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.enums.AccountStatus;
import com.pfe.docextraction.enums.UserRole;
import com.pfe.docextraction.repository.UserRepository;
import com.pfe.docextraction.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Forcer le rôle à LECTEUR pour toutes les inscriptions (prévention des erreurs côté client)
        UserRole assignedRole = UserRole.LECTEUR;
        log.info("Assigning default role='{}' for new registration email={}", assignedRole, request.getEmail());

        User user = User.builder()
                .username(request.getUsername())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .accountStatus(AccountStatus.PENDING)
                .isActive(true)
                .isLocked(false)
                .failedAttempts(0)
                .build();

        userRepository.save(user);
        log.info("📝 Nouvel utilisateur en attente : {}", normalizedEmail);

        emailService.sendNewAccountNotification(request.getUsername(), normalizedEmail);
        return new AuthResponse(null, user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(AuthRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        log.debug("🔐 Tentative de connexion pour : {}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

       
        if (user.getAccountStatus() == AccountStatus.PENDING) {
            throw new RuntimeException("Compte en attente d'approbation par l'administrateur");
        }
        if (user.getAccountStatus() == AccountStatus.REJECTED) {
            throw new RuntimeException("Compte rejeté — contactez l'administrateur");
        }

       
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            log.warn("🔒 Tentative de connexion sur un compte bloqué : {}", normalizedEmail);
            throw new RuntimeException("Compte bloqué — contactez l'administrateur pour le débloquer");
        }

        
        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.warn("🚫 Tentative de connexion sur un compte désactivé : {}", normalizedEmail);
            throw new RuntimeException("Compte désactivé — contactez l'administrateur pour le réactiver");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
          
            int attempts = user.getFailedAttempts() == null ? 0 : user.getFailedAttempts();
            attempts++;
            user.setFailedAttempts(attempts);

          
            if (attempts >= 5) {
                user.setIsLocked(true);
                userRepository.save(user);
                log.warn("🔒 Compte auto-bloqué après {} tentatives : {}", attempts, normalizedEmail);
                throw new RuntimeException("Trop de tentatives échouées — compte bloqué automatiquement");
            }
            userRepository.save(user);
            throw new RuntimeException("Email ou mot de passe incorrect (" + attempts + "/5)");
        }

     
        if (user.getFailedAttempts() != null && user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}