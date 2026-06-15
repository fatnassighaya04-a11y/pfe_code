package com.pfe.docextraction.controller;

import com.pfe.docextraction.entity.Document;
import com.pfe.docextraction.entity.Extraction;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.ExtractionStatus;
import com.pfe.docextraction.exception.DuplicateDocumentException;
import com.pfe.docextraction.repository.DocumentRepository;
import com.pfe.docextraction.repository.ExtractionRepository;
import com.pfe.docextraction.service.audit.AuditService;
import com.pfe.docextraction.service.document.DocumentService;
import com.pfe.docextraction.service.extraction.ExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final ExtractionService extractionService;
    private final ExtractionRepository extractionRepository;
    private final AuditService auditService;

   
    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateDocumentException e) {
        Document existing = e.getExistingDocument();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "DUPLICATE_DOCUMENT");
        body.put("message", "Ce document a déjà été uploadé");
        body.put("existingDocument", Map.of(
            "id", existing.getId(),
            "filename", existing.getOriginalFilename(),
            "uploadedAt", existing.getCreatedAt(),
            "uploadedBy", existing.getUploadedBy() != null ? existing.getUploadedBy().getEmail() : null,
            "status", existing.getStatus()
        ));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threshold", defaultValue = "85") double threshold,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) throws IOException {

        Document document = documentService.uploadDocument(file, user);

        auditService.log(
            user,
            "DOCUMENT_UPLOAD",
            "DOCUMENT",
            document.getId().toString(),
            "SUCCESS",
            httpRequest.getRemoteAddr(),
            "Fichier uploadé : " + document.getOriginalFilename()
        );

        
        Extraction pendingExtraction = Extraction.builder()
                .document(document)
                .status(ExtractionStatus.PENDING)
                .extractionModel("pending")
                .autoValidationThreshold(threshold)
                .build();
        extractionRepository.save(pendingExtraction);

        auditService.log(
            user,
            "EXTRACTION_START",
            "EXTRACTION",
            pendingExtraction.getId().toString(),
            "SUCCESS",
            httpRequest.getRemoteAddr(),
            "Extraction lancée pour : " + document.getOriginalFilename()
        );

       
        CompletableFuture.runAsync(() -> extractionService.extractDocument(document.getId(), threshold));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", document.getId());
        response.put("filename", document.getOriginalFilename());
        response.put("fileType", document.getFileType());
        response.put("fileSize", document.getFileSize());
        response.put("status", document.getStatus());
        response.put("uploadedAt", document.getCreatedAt());
        response.put("documentType", document.getDocumentType());
        response.put("autoValidationThreshold", threshold);
        response.put("message", "Extraction IA lancée via OpenRouter Gemini — seuil: " + threshold + "%");

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDocuments(
            @RequestParam(required = false) String status) {

        List<Document> documents = documentService.getAllDocuments();

        if (status != null && !status.isBlank()) {
            try {
                DocumentStatus filter = DocumentStatus.valueOf(status.toUpperCase());
                documents = documents.stream()
                        .filter(d -> d.getStatus() == filter)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(buildDocumentList(documents));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentDocuments(
            @RequestParam(defaultValue = "5") int limit) {

        List<Document> recent = documentService.getAllDocuments().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(limit)
                .collect(Collectors.toList());
        return ResponseEntity.ok(buildDocumentList(recent));
    }

    @GetMapping("/to-validate")
    public ResponseEntity<List<Map<String, Object>>> getDocumentsToValidate() {
        List<Document> documents = documentService.getAllDocuments().stream()
                .filter(d -> d.getStatus() == DocumentStatus.COMPLETED)
                .collect(Collectors.toList());
        return ResponseEntity.ok(buildDocumentList(documents));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String id) {
        Document document = documentService.getDocumentById(UUID.fromString(id));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", document.getId());
        response.put("filename", document.getOriginalFilename());
        response.put("fileType", document.getFileType());
        response.put("fileSize", document.getFileSize());
        response.put("status", document.getStatus());
        response.put("uploadedAt", document.getCreatedAt());
        response.put("documentType", document.getDocumentType());

        if (document.getUploadedBy() != null) {
            response.put("uploadedByEmail", document.getUploadedBy().getEmail());
        }

        extractionRepository.findByDocumentIdOrderByCreatedAtDesc(document.getId())
                .stream().findFirst()
                .ifPresent(ext -> {
                    if (ext.getConfidenceScore() != null) {
                        response.put("confidenceScore", ext.getConfidenceScore().doubleValue());
                    }
                });

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getDocumentFile(@PathVariable String id) throws IOException {
        Document document = documentService.getDocumentById(UUID.fromString(id));

        Path filePath = Paths.get(document.getFilePath()).normalize();
        if (!filePath.isAbsolute()) {
            filePath = Paths.get("").toAbsolutePath().resolve(filePath).normalize();
        }

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        auditService.log(
            document.getUploadedBy(),
            "DOCUMENT_DOWNLOAD",
            "DOCUMENT",
            document.getId().toString(),
            "SUCCESS",
            null,
            "Téléchargement / aperçu du fichier : " + document.getOriginalFilename()
        );

        byte[] fileBytes = Files.readAllBytes(filePath);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(document.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFilename() + "\"")
                .contentType(mediaType)
                .body(fileBytes);
    }

    private List<Map<String, Object>> buildDocumentList(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", doc.getId());
                    map.put("filename", doc.getOriginalFilename());
                    map.put("fileType", doc.getFileType());
                    map.put("fileSize", doc.getFileSize());
                    map.put("status", doc.getStatus());
                    map.put("documentType", doc.getDocumentType());
                    map.put("uploadedAt", doc.getCreatedAt());

                    if (doc.getUploadedBy() != null) {
                        map.put("uploadedByEmail", doc.getUploadedBy().getEmail());
                    }

                    extractionRepository.findByDocumentIdOrderByCreatedAtDesc(doc.getId())
                            .stream().findFirst()
                            .ifPresent(ext -> {
                                if (ext.getConfidenceScore() != null) {
                                    map.put("confidenceScore", ext.getConfidenceScore().doubleValue());
                                }
                            });
                    return map;
                })
                .collect(Collectors.toList());
    }
}