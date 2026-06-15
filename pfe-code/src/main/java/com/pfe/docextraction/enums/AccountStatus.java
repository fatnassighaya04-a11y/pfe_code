package com.pfe.docextraction.enums;

public enum AccountStatus {
    // Compte créé, en attente d'approbation admin
    PENDING,

    // Compte approuvé — peut se connecter
    ACTIVE,

    // Compte rejeté par l'admin
    REJECTED
}