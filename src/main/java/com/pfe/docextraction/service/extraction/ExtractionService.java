package com.pfe.docextraction.service.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.docextraction.entity.Document;
import com.pfe.docextraction.entity.Extraction;
import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.ExtractionStatus;
import com.pfe.docextraction.enums.FileType;
import com.pfe.docextraction.repository.DocumentRepository;
import com.pfe.docextraction.repository.ExtractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private static final String OPENROUTER_MODEL = "openrouter-gemini";

    private final ExtractionRepository extractionRepository;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final GeminiService geminiService;
    private final OcrService ocrService;

    @Value("${app.auto-validation.threshold:85}")
    private double defaultAutoValidationThreshold;

    @Value("${file.storage.location}")
    private String storageLocation;

    @PostConstruct
    public void init() {
        File dir = new File(storageLocation);
        if (!dir.exists()) dir.mkdirs();
    }

    public void extractDocument(UUID documentId, double customThreshold) {
        long startedAt = System.currentTimeMillis();

        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Extraction extraction = extractionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                    .stream().findFirst().orElse(null);

            if (extraction == null) {
                extraction = Extraction.builder()
                        .document(document)
                        .status(ExtractionStatus.IN_PROGRESS)
                        .extractionModel(OPENROUTER_MODEL)
                        .autoValidationThreshold(customThreshold)
                        .build();
                extractionRepository.save(extraction);
            }

            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            String documentContext = buildDocumentContext(document);
            
            // CORRECTION 1: Déclarer la variable sourceText
            String sourceText = "";
            String modelResponse;

            if (isImage(document.getFileType())) {
                byte[] imageBytes = Files.readAllBytes(resolveDocumentPath(document));
                modelResponse = geminiService.extractDataFromImage(imageBytes, document.getFileType().getMimeType(), documentContext);
                sourceText = "[Image document] " + document.getOriginalFilename();
            } else {
                // CORRECTION 2: Extraire le texte source avant de l'utiliser
                sourceText = extractSourceText(document);
                modelResponse = geminiService.extractData(sourceText, documentContext);
            }

            JsonNode responseNode = objectMapper.readTree(modelResponse);
            JsonNode extractedNode = responseNode.path("extracted_data");
            if (extractedNode.isMissingNode() || extractedNode.isNull()) {
                extractedNode = responseNode;
            }

            String extractedJson = objectMapper.writeValueAsString(extractedNode);

            JsonNode scoreNode = responseNode.path("confidence_score");
            if (scoreNode.isMissingNode() || scoreNode.isNull()) {
                scoreNode = extractedNode.path("confidence_score");
            }
            double confidenceValue = scoreNode.asDouble(-1.0);

            if (confidenceValue > 0 && confidenceValue <= 1.0) {
                confidenceValue = confidenceValue * 100;
            }

            if (confidenceValue < 0) {
                confidenceValue = estimateConfidence(extractedNode);
            }
           
            confidenceValue = Math.max(0, Math.min(100, confidenceValue));

            BigDecimal score = BigDecimal.valueOf(confidenceValue).setScale(2, RoundingMode.HALF_UP);

            extraction.setExtractedData(extractedJson);
            extraction.setRawText(sourceText);
            extraction.setConfidenceScore(score);
            extraction.setExtractionDurationMs((int) (System.currentTimeMillis() - startedAt));
            extraction.setExtractionModel(OPENROUTER_MODEL);
            extraction.setStatus(ExtractionStatus.SUCCESS);

            double threshold = (customThreshold >= 0 && customThreshold <= 100) ? customThreshold : defaultAutoValidationThreshold;
            if (confidenceValue >= threshold) {
                extraction.setValidatedAt(LocalDateTime.now());
                extraction.setValidationNotes(
                        "Approuvé automatiquement (IA) — score " + score + "% ≥ seuil " + threshold + "%"
                );
            }

            document.setStatus(confidenceValue >= threshold
                    ? DocumentStatus.VALIDATED
                    : DocumentStatus.COMPLETED);
            extractionRepository.save(extraction);
            documentRepository.save(document);

        } catch (Exception e) {
            log.error("Erreur lors de l'extraction: {}", e.getMessage(), e);
            try {
                Document document = documentRepository.findById(documentId).orElse(null);
                if (document != null) {
                    Extraction extraction = extractionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                            .stream().findFirst().orElse(null);
                    if (extraction != null) {
                        extraction.setStatus(ExtractionStatus.FAILED);
                        extraction.setErrorMessage(e.getMessage());
                        extraction.setConfidenceScore(BigDecimal.ZERO);
                        extraction.setExtractedData("{\"erreur\":\"" + e.getMessage() + "\"}");
                        extractionRepository.save(extraction);
                        document.setStatus(DocumentStatus.FAILED);
                        documentRepository.save(document);
                    }
                }
            } catch (Exception ex) {
                log.error("Erreur lors de la gestion de l'échec: {}", ex.getMessage(), ex);
            }
        }
    }

    public Extraction updateFields(UUID extractionId, Map<String, Object> updatedFields) {
        Extraction extraction = extractionRepository.findById(extractionId)
                .orElseThrow(() -> new RuntimeException("Extraction non trouvée"));
        try {
            Map<String, Object> currentData = objectMapper.readValue(extraction.getExtractedData(), Map.class);
            currentData.putAll(updatedFields);
            String updatedJson = objectMapper.writeValueAsString(currentData);
            extraction.setExtractedData(updatedJson);
        } catch (Exception e) {
            throw new RuntimeException("Erreur mise à jour des champs", e);
        }
        return extractionRepository.save(extraction);
    }

    public void approve(UUID extractionId) {
        Extraction extraction = extractionRepository.findById(extractionId)
                .orElseThrow(() -> new RuntimeException("Extraction non trouvée"));
        extraction.setStatus(ExtractionStatus.SUCCESS);
        extraction.setValidatedAt(LocalDateTime.now());
        extraction.setValidationNotes("Validé manuellement après correction");
        extractionRepository.save(extraction);

        Document document = extraction.getDocument();
        document.setStatus(DocumentStatus.VALIDATED);
        documentRepository.save(document);
    }

    public void reject(UUID extractionId, String reason, String comment) {
        Extraction extraction = extractionRepository.findById(extractionId)
                .orElseThrow(() -> new RuntimeException("Extraction non trouvée"));
        extraction.setStatus(ExtractionStatus.FAILED);
        String fullNote = "Rejeté : " + reason;
        if (comment != null && !comment.isBlank()) fullNote += " - " + comment;
        extraction.setValidationNotes(fullNote);
        extractionRepository.save(extraction);

        Document document = extraction.getDocument();
        document.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(document);
    }

    public Extraction findByDocumentId(UUID documentId) {
        return extractionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                .stream().findFirst().orElse(null);
    }

    private double estimateConfidence(JsonNode extractedNode) {
        if (extractedNode == null || !extractedNode.isObject() || extractedNode.size() == 0) {
            return 0.0;
        }
        int total = 0;
        int filled = 0;
        var fields = extractedNode.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            JsonNode value = entry.getValue();
            total++;
            if (value != null && !value.isNull() && !value.asText("").isBlank()) {
                filled++;
            }
        }
        if (total == 0) return 0.0;
        double ratio = (double) filled / total;
        return Math.max(50.0, Math.min(95.0, ratio * 100));
    }

    private String extractSourceText(Document document) throws IOException {
        Path filePath = resolveDocumentPath(document);
        FileType fileType = document.getFileType();

        if (fileType == FileType.PDF) {
            try (var pdfDocument = Loader.loadPDF(filePath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(pdfDocument);
                if (text != null && !text.isBlank() && text.trim().length() > 30) {
                    return text.trim();
                }
            }
           
            if (ocrService.isAvailable()) {
                log.info("📄 PDF sans couche texte détecté → OCR sur {}", document.getOriginalFilename());
                String ocrText = ocrService.ocrPdf(filePath.toFile());
                if (ocrText != null && !ocrText.isBlank()) {
                    return ocrText;
                }
            }
            return "[PDF scanné non lisible — OCR indisponible ou échec]";
        }

        if (fileType == FileType.TXT) {
            return Files.readString(filePath).trim();
        }

        if (isImage(fileType)) {
            if (ocrService.isAvailable()) {
                String ocrText = ocrService.ocrImage(filePath.toFile());
                if (ocrText != null && !ocrText.isBlank()) {
                    return ocrText;
                }
            }
            return "[Image document] " + document.getOriginalFilename();
        }

        return "[Document non textuel] " + document.getOriginalFilename();
    }

    private Path resolveDocumentPath(Document document) {
        Path filePath = Paths.get(document.getFilePath()).normalize();
        if (!filePath.isAbsolute()) {
            filePath = Paths.get("").toAbsolutePath().resolve(filePath).normalize();
        }
        return filePath;
    }

    private boolean isImage(FileType fileType) {
        return fileType == FileType.PNG
                || fileType == FileType.JPG
                || fileType == FileType.JPEG
                || fileType == FileType.TIFF
                || fileType == FileType.BMP;
    }

    private String buildDocumentContext(Document document) {
        return "Nom du fichier: " + document.getOriginalFilename()
                + " | Type fichier: " + document.getFileType()
                + " | Type document: " + document.getDocumentType();
    }
}