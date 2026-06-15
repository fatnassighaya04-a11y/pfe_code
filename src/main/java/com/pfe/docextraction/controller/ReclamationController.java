package com.pfe.docextraction.controller;

import com.pfe.docextraction.dto.CreateReclamationRequest;
import com.pfe.docextraction.dto.MessageRequest;
import com.pfe.docextraction.dto.ReclamationResponse;
import com.pfe.docextraction.dto.StatusUpdateRequest;
import com.pfe.docextraction.entity.Reclamation;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.service.ReclamationService;
import com.pfe.docextraction.service.audit.AuditService;
import com.pfe.docextraction.service.auth.JwtService;
import com.pfe.docextraction.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
@Slf4j
public class ReclamationController {

    private final ReclamationService reclamationService;
    private final AuditService auditService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private UUID getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));
            return user.getId();
        }
        throw new RuntimeException("Token manquant ou invalide");
    }

    private String getCurrentUserEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.extractEmail(token);
        }
        throw new RuntimeException("Token manquant ou invalide");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATEUR', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<?> creerReclamation(
            @Valid @RequestBody CreateReclamationRequest request,
            HttpServletRequest servletRequest) {
        try {
            UUID userId = getCurrentUserId(servletRequest);
            String auteurEmail = getCurrentUserEmail(servletRequest);
            Reclamation reclamation = reclamationService.creerReclamation(userId, request);
            userRepository.findByEmail(auteurEmail).ifPresent(user ->
                    auditService.logAction(user, "RECLAMATION_CREATE", "RECLAMATION", reclamation.getId().toString()));
            return ResponseEntity.status(HttpStatus.CREATED).body(reclamation);
        } catch (Exception e) {
            log.error("Erreur création réclamation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'GESTIONNAIRE', 'ADMIN', 'LECTEUR')")
    public ResponseEntity<?> getMesReclamations(
            @PageableDefault(size = 10, sort = "dateCreation", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        try {
            UUID userId = getCurrentUserId(request);
            Page<Reclamation> page = reclamationService.getMesReclamations(userId, pageable);

            // Réponse simplifiée et stable pour le frontend
            List<Map<String, Object>> simplifiedList = page.getContent().stream().map(reclamation -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", reclamation.getId());
                map.put("userId", reclamation.getUserId());
                map.put("sujet", reclamation.getSujet());
                map.put("type", reclamation.getType());

                String description = reclamation.getDescription();
                if (description != null) {
                    if (description.length() > 200) {
                        description = description.substring(0, 200) + "...";
                    }
                    description = description.replace("\"", "'")
                            .replace("\n", " ")
                            .replace("\r", " ")
                            .replace("\t", " ");
                }
                map.put("description", description);
                map.put("priorite", reclamation.getPriorite() != null ? reclamation.getPriorite().name() : "MOYENNE");
                map.put("statut", reclamation.getStatut() != null ? reclamation.getStatut().name() : "EN_ATTENTE");
                map.put("dateCreation", reclamation.getDateCreation());
                map.put("dateModification", reclamation.getDateModification());
                map.put("dateResolution", reclamation.getDateResolution());
                map.put("pieceJointe", reclamation.getPieceJointe());

                try {
                    userRepository.findById(reclamation.getUserId()).ifPresent(user -> {
                        map.put("emailUtilisateur", user.getEmail());
                        map.put("nomUtilisateur", user.getDisplayName());
                        map.put("roleUtilisateur", user.getRole() != null ? user.getRole().name() : null);
                    });
                } catch (Exception e) {
                    map.put("emailUtilisateur", null);
                    map.put("nomUtilisateur", null);
                    map.put("roleUtilisateur", null);
                }

                return map;
            }).collect(Collectors.toList());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("content", simplifiedList);
            response.put("totalElements", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("size", page.getSize());
            response.put("number", page.getNumber());
            response.put("first", page.isFirst());
            response.put("last", page.isLast());
            response.put("empty", page.isEmpty());

            log.info("Réclamations utilisateur {}: {} trouvée(s)", userId, page.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération réclamations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllReclamations(
            @PageableDefault(size = 10, sort = "dateCreation", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<Reclamation> page = reclamationService.getAllReclamations(pageable);
            
            // Créer une réponse simplifiée pour éviter les problèmes JSON
            List<Map<String, Object>> simplifiedList = page.getContent().stream().map(reclamation -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", reclamation.getId());
                map.put("userId", reclamation.getUserId());
                map.put("sujet", reclamation.getSujet());
                map.put("type", reclamation.getType());
                
                // Nettoyer la description : limiter la longueur et échapper
                String description = reclamation.getDescription();
                if (description != null) {
                    if (description.length() > 200) {
                        description = description.substring(0, 200) + "...";
                    }
                    // Remplacer les caractères problématiques
                    description = description.replace("\"", "'")
                                             .replace("\n", " ")
                                             .replace("\r", " ")
                                             .replace("\t", " ");
                }
                map.put("description", description);
                map.put("priorite", reclamation.getPriorite() != null ? reclamation.getPriorite().name() : "MOYENNE");
                map.put("statut", reclamation.getStatut() != null ? reclamation.getStatut().name() : "EN_ATTENTE");
                map.put("dateCreation", reclamation.getDateCreation());
                map.put("dateModification", reclamation.getDateModification());
                map.put("dateResolution", reclamation.getDateResolution());
                    map.put("pieceJointe", reclamation.getPieceJointe());
                
                // Récupérer l'email de l'utilisateur
                try {
                    userRepository.findById(reclamation.getUserId()).ifPresent(user -> {
                        map.put("emailUtilisateur", user.getEmail());
                        map.put("nomUtilisateur", user.getDisplayName());
                        map.put("roleUtilisateur", user.getRole() != null ? user.getRole().name() : null);
                    });
                } catch (Exception e) {
                    map.put("emailUtilisateur", null);
                    map.put("nomUtilisateur", null);
                    map.put("roleUtilisateur", null);
                }
                
                return map;
            }).collect(Collectors.toList());
            
            // Construire la réponse paginée
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("content", simplifiedList);
            response.put("totalElements", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("size", page.getSize());
            response.put("number", page.getNumber());
            response.put("first", page.isFirst());
            response.put("last", page.isLast());
            response.put("empty", page.isEmpty());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération toutes les réclamations: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des réclamations: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'GESTIONNAIRE', 'ADMIN', 'LECTEUR')")
    public ResponseEntity<?> getReclamationDetail(@PathVariable UUID id) {
        try {
            ReclamationResponse response = reclamationService.getReclamationDetailResponse(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération détail réclamation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Réclamation non trouvée"));
        }
    }

    @PostMapping("/{id}/response")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> repondreAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody MessageRequest request,
            HttpServletRequest servletRequest) {
        try {
            String auteurEmail = getCurrentUserEmail(servletRequest);
            ReclamationResponse response = reclamationService.ajouterMessage(id, request, auteurEmail, true);
            userRepository.findByEmail(auteurEmail).ifPresent(user ->
                    auditService.logAction(user, "RECLAMATION_ADMIN_RESPONSE", "RECLAMATION", id.toString()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur ajout réponse admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/comment")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'GESTIONNAIRE', 'ADMIN', 'LECTEUR')")
    public ResponseEntity<?> ajouterCommentaire(
            @PathVariable UUID id,
            @Valid @RequestBody MessageRequest request,
            HttpServletRequest servletRequest) {
        try {
            String auteurEmail = getCurrentUserEmail(servletRequest);
            ReclamationResponse response = reclamationService.ajouterMessage(id, request, auteurEmail, false);
            userRepository.findByEmail(auteurEmail).ifPresent(user ->
                    auditService.logAction(user, "RECLAMATION_COMMENT", "RECLAMATION", id.toString()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur ajout commentaire: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changerStatut(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest servletRequest) {
        try {
            String auteurEmail = getCurrentUserEmail(servletRequest);
            ReclamationResponse response = reclamationService.changerStatut(id, request, auteurEmail);
            userRepository.findByEmail(auteurEmail).ifPresent(user ->
                    auditService.logAction(user, "RECLAMATION_STATUS_CHANGE", "RECLAMATION", id.toString()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur mise à jour statut réclamation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
 
