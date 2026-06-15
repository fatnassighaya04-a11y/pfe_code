package com.pfe.docextraction.exception;

import com.pfe.docextraction.entity.Document;
import lombok.Getter;

/**
 * Levée quand un document avec le même hash SHA-256 a déjà été uploadé.
 */
@Getter
public class DuplicateDocumentException extends RuntimeException {
    private final Document existingDocument;

    public DuplicateDocumentException(Document existingDocument) {
        super("Ce document a déjà été uploadé.");
        this.existingDocument = existingDocument;
    }
}
