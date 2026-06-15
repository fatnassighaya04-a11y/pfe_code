package com.pfe.docextraction.exception;

import com.pfe.docextraction.entity.Document;
import lombok.Getter;


@Getter
public class DuplicateDocumentException extends RuntimeException {
    private final Document existingDocument;

    public DuplicateDocumentException(Document existingDocument) {
        super("Ce document a déjà été uploadé.");
        this.existingDocument = existingDocument;
    }
}
