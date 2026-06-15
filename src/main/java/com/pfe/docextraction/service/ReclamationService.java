package com.pfe.docextraction.service;

import com.pfe.docextraction.dto.*;
import com.pfe.docextraction.entity.*;
import com.pfe.docextraction.repository.ReclamationMessageRepository;
import com.pfe.docextraction.repository.ReclamationRepository;
import com.pfe.docextraction.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ReclamationMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public Reclamation creerReclamation(UUID userId, CreateReclamationRequest request) {
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ReclamationPriority priority;
        try {
            priority = ReclamationPriority.valueOf(request.getPriorite().toUpperCase());
        } catch (Exception e) {
            priority = ReclamationPriority.MOYENNE;
        }

        Reclamation reclamation = Reclamation.builder()
                .userId(userId)
                .sujet(request.getSujet())
            .type(request.getType())
                .description(request.getDescription())
                .pieceJointe(request.getPieceJointe())
                .priorite(priority)
                .statut(ReclamationStatus.EN_ATTENTE)
                .build();

        Reclamation saved = reclamationRepository.save(reclamation);
        log.info("Nouvelle réclamation créée : {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Reclamation> getMesReclamations(UUID userId, Pageable pageable) {
        return reclamationRepository.findByUserIdOrderByDateCreationDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Reclamation> getAllReclamations(Pageable pageable) {
        return reclamationRepository.findAllByOrderByDateCreationDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Reclamation getReclamationDetail(UUID id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));
    }

    @Transactional(readOnly = true)
    public ReclamationResponse getReclamationDetailResponse(UUID id) {
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));
        return mapToResponse(reclamation);
    }

    @Transactional
    public ReclamationResponse ajouterMessage(UUID id, MessageRequest request, String auteurEmail, boolean estAdmin) {
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));
        
        ReclamationMessage message = ReclamationMessage.builder()
                .reclamation(reclamation)
                .auteur(auteurEmail)
                .contenu(request.getContenu())
                .estReponseAdmin(estAdmin)
                .build();
        messageRepository.save(message);
        if (reclamation.getMessages() == null) {
            reclamation.setMessages(new java.util.ArrayList<>());
        }
        reclamation.getMessages().add(message);
        
        if (estAdmin && reclamation.getStatut() == ReclamationStatus.EN_ATTENTE) {
            reclamation.setStatut(ReclamationStatus.EN_COURS);
            reclamation = reclamationRepository.save(reclamation);
        }
        
        log.info("Message ajouté à {} par {}", id, auteurEmail);
        return mapToResponse(reclamation);
    }

    @Transactional
    public ReclamationResponse changerStatut(UUID id, StatusUpdateRequest request, String auteurEmail) {
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));

        ReclamationStatus ancienStatut = reclamation.getStatut();
        reclamation.setStatut(request.getStatut());

        if (request.getStatut() == ReclamationStatus.RESOLUE) {
            reclamation.setDateResolution(LocalDateTime.now());
        } else if (ancienStatut == ReclamationStatus.RESOLUE) {
            reclamation.setDateResolution(null);
        }

        if (request.getCommentaire() != null && !request.getCommentaire().trim().isEmpty()) {
            ReclamationMessage message = ReclamationMessage.builder()
                    .reclamation(reclamation)
                    .auteur(auteurEmail)
                    .contenu(request.getCommentaire().trim())
                    .estReponseAdmin(true)
                    .build();
            messageRepository.save(message);
            if (reclamation.getMessages() == null) {
                reclamation.setMessages(new java.util.ArrayList<>());
            }
            reclamation.getMessages().add(message);
        }

        reclamation = reclamationRepository.save(reclamation);
        log.info("Statut mis à jour pour {} par {}: {} -> {}", id, auteurEmail, ancienStatut, request.getStatut());
        return mapToResponse(reclamation);
    }

    /**
     * Échappe les caractères spéciaux pour le JSON
     */
    private String escapeJson(String value) {
        if (value == null) return null;
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Nettoie la description (trop longue ou avec caractères problématiques)
     */
    private String cleanDescription(String description) {
        if (description == null) return null;
        // Limiter la longueur à 500 caractères
        if (description.length() > 500) {
            description = description.substring(0, 500) + "...";
        }
        return description;
    }

    private ReclamationResponse mapToResponse(Reclamation r) {
        // Nettoyer et échapper les champs problématiques
        String cleanedSujet = escapeJson(r.getSujet());
        String cleanedDescription = cleanDescription(escapeJson(r.getDescription()));
        User user = userRepository.findById(r.getUserId()).orElse(null);
        
        return ReclamationResponse.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .sujet(cleanedSujet)
            .type(escapeJson(r.getType()))
                .description(cleanedDescription)
                .priorite(r.getPriorite())
                .statut(r.getStatut())
                .dateCreation(r.getDateCreation())
                .dateModification(r.getDateModification())
                .dateResolution(r.getDateResolution())
            .emailUtilisateur(user != null ? user.getEmail() : null)
            .nomUtilisateur(user != null ? user.getDisplayName() : null)
            .roleUtilisateur(user != null && user.getRole() != null ? user.getRole().name() : null)
            .pieceJointe(r.getPieceJointe())
                .messages(r.getMessages() != null ? r.getMessages().stream()
                        .map(m -> MessageResponse.builder()
                                .id(m.getId())
                                .auteur(escapeJson(m.getAuteur()))
                                .contenu(escapeJson(m.getContenu()))
                                .estReponseAdmin(m.isEstReponseAdmin())
                                .dateMessage(m.getDateMessage())
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private ReclamationListResponse mapToListResponse(Reclamation r) {
        return ReclamationListResponse.builder()
                .id(r.getId())
                .sujet(escapeJson(r.getSujet()))
                .type(escapeJson(r.getType()))
                .priorite(r.getPriorite())
                .statut(r.getStatut())
                .dateCreation(r.getDateCreation())
                .pieceJointe(r.getPieceJointe())
                .build();
    }
}
