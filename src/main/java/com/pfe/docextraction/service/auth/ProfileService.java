package com.pfe.docextraction.service.auth;

import com.pfe.docextraction.dto.ProfileResponse;
import com.pfe.docextraction.dto.ProfileUpdateRequest;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.repository.UserRepository;
import com.pfe.docextraction.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    public ProfileResponse getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .role(user.getRole().name())
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : "PENDING")
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        StringBuilder oldData = new StringBuilder();
        StringBuilder newData = new StringBuilder();
        boolean hasChanges = false;

        // Vérifier et mettre à jour le nom d'utilisateur
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            oldData.append("Nom: ").append(user.getUsername()).append("\n");
            newData.append("Nom: ").append(request.getUsername()).append("\n");
            user.setUsername(request.getUsername());
            hasChanges = true;
        }

        // Vérifier et mettre à jour l'email
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Vérifier si l'email est déjà utilisé par un autre utilisateur
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Cet email est déjà utilisé");
            }
            oldData.append("Email: ").append(user.getEmail()).append("\n");
            newData.append("Email: ").append(request.getEmail()).append("\n");
            user.setEmail(request.getEmail());
            hasChanges = true;
        }

        // Vérifier et mettre à jour le téléphone
        if (request.getTelephone() != null && 
            (user.getTelephone() == null || !request.getTelephone().equals(user.getTelephone()))) {
            oldData.append("Téléphone: ").append(user.getTelephone() != null ? user.getTelephone() : "Non renseigné").append("\n");
            newData.append("Téléphone: ").append(request.getTelephone()).append("\n");
            user.setTelephone(request.getTelephone());
            hasChanges = true;
        }

        User savedUser = userRepository.save(user);

        // Envoyer les notifications par email si des changements ont été détectés
        if (hasChanges) {
            // Email à l'utilisateur
            emailService.sendProfileModificationToUser(
                savedUser.getUsername(),
                savedUser.getEmail(),
                oldData.toString(),
                newData.toString()
            );
            
            // Email à l'admin
            emailService.sendProfileModificationToAdmin(
                savedUser.getUsername(),
                savedUser.getEmail(),
                oldData.toString(),
                newData.toString()
            );
        }

        return ProfileResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .telephone(savedUser.getTelephone())
                .role(savedUser.getRole().name())
                .accountStatus(savedUser.getAccountStatus() != null ? savedUser.getAccountStatus().name() : "PENDING")
                .createdAt(savedUser.getCreatedAt())
                .build();
    }
}