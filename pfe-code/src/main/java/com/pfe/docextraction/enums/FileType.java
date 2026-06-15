// ================================================================
// FICHIER : src/main/java/com/pfe/docextraction/enums/FileType.java
// RÔLE    : Formats de fichiers acceptés par la plateforme
// ================================================================
package com.pfe.docextraction.enums;

import lombok.Getter;

@Getter
public enum FileType {

    PDF("application/pdf", ".pdf"),
    PNG("image/png", ".png"),
    JPG("image/jpeg", ".jpg"),
    JPEG("image/jpeg", ".jpeg"),
    TIFF("image/tiff", ".tiff"),
    BMP("image/bmp", ".bmp"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    TXT("text/plain", ".txt");

    // Type MIME officiel du fichier
    private final String mimeType;

    // Extension de fichier associée
    private final String extension;

    FileType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    // Méthode utilitaire : retrouver un FileType depuis son type MIME
    // Exemple : FileType.fromMimeType("image/png") → FileType.PNG
    public static FileType fromMimeType(String mimeType) {
        for (FileType type : values()) {
            if (type.mimeType.equalsIgnoreCase(mimeType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type MIME non supporté : " + mimeType);
    }
}
