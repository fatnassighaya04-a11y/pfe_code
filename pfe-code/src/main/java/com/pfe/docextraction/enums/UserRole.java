// ================================================================
// FICHIER : src/main/java/com/pfe/docextraction/enums/UserRole.java
// RÔLE    : Définit les 4 rôles possibles pour un utilisateur
// ================================================================
package com.pfe.docextraction.enums;

public enum UserRole {
    // Accès total : gestion users, tous documents, configuration
    ADMIN,

    // Upload, voir toutes extractions, valider, exporter rapports
    GESTIONNAIRE,

    // Upload ses propres documents, valider ses extractions
    OPERATEUR,

    // Lecture seule : voit uniquement les extractions validées
    LECTEUR
}
