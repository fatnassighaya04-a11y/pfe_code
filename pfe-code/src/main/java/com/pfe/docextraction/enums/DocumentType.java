// ================================================================
// FICHIER : src/main/java/com/pfe/docextraction/enums/DocumentType.java
// RÔLE    : Types de documents administratifs supportés
//           Chaque type a un prompt Gemini différent
// ================================================================
package com.pfe.docextraction.enums;

public enum DocumentType {
    // Facture fournisseur / client
    FACTURE,

    // Bon de commande
    BON_COMMANDE,

    // Contrat commercial ou de travail
    CONTRAT,

    // Formulaire administratif
    FORMULAIRE,

    // Document texte générique
    DOCUMENT_TEXTE,

    // Tableau de données / rapport Excel
    TABLEAU_DONNEES,

    // Type non encore déterminé (détection automatique en cours)
    AUTRE
}
