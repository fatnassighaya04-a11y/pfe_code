
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

   
    private final String mimeType;

  
    private final String extension;

    FileType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }


    public static FileType fromMimeType(String mimeType) {
        for (FileType type : values()) {
            if (type.mimeType.equalsIgnoreCase(mimeType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type MIME non supporté : " + mimeType);
    }
}
