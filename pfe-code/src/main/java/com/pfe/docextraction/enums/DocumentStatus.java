package com.pfe.docextraction.enums;

public enum DocumentStatus {
    UPLOADED,     // <-- ADD THIS LINE
    PENDING,      // en attente d'upload/extraction
    PROCESSING,   // extraction en cours
    COMPLETED,    // extraction terminée, en attente validation manuelle (score < seuil)
    VALIDATED,    // validé automatiquement ou manuellement
    REJECTED,     // rejeté
    FAILED        // erreur
}