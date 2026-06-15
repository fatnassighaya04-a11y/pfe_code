package com.pfe.docextraction.service.document;

import com.pfe.docextraction.entity.Document;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.DocumentType;
import com.pfe.docextraction.enums.FileType;
import com.pfe.docextraction.exception.DuplicateDocumentException;
import com.pfe.docextraction.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    // Upload un document
    public Document uploadDocument(MultipartFile file, User user) throws IOException {

        // 1. Calculer le SHA-256 du contenu
        byte[] fileBytes = file.getBytes();
        String checksum = computeSha256(fileBytes);

        // 2. Vérifier si un document avec ce hash existe déjà
        Optional<Document> existing = documentRepository.findByChecksumAndDeletedAtIsNull(checksum);
        if (existing.isPresent()) {
            throw new DuplicateDocumentException(existing.get());
        }

        // 3. Sauvegarder le fichier sur le disque
        String storedFilename = fileStorageService.saveFile(file);

        // 4. Détecter le type de fichier
        FileType fileType = detectFileType(file.getOriginalFilename());

        // 5. Créer l'entrée en base de données
        Document document = Document.builder()
            .originalFilename(file.getOriginalFilename())
            .storedFilename(storedFilename)
            .filePath("uploads/" + storedFilename)
            .fileType(fileType)
            .fileSize(file.getSize())
            .mimeType(file.getContentType())
            .checksum(checksum)
            .status(DocumentStatus.UPLOADED)
            .documentType(DocumentType.AUTRE)
            .uploadedBy(user)
            .build();

        return documentRepository.save(document);
    }

    // Récupérer tous les documents
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // Calcul SHA-256 → hex 64 caractères
    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    // Détecter le type depuis l'extension
    private FileType detectFileType(String filename) {
        if (filename == null) return FileType.PDF;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return FileType.PDF;
        if (lower.endsWith(".png")) return FileType.PNG;
        if (lower.endsWith(".jpg")) return FileType.JPG;
        if (lower.endsWith(".jpeg")) return FileType.JPEG;
        if (lower.endsWith(".docx")) return FileType.DOCX;
        if (lower.endsWith(".xlsx")) return FileType.XLSX;
        if (lower.endsWith(".txt")) return FileType.TXT;
        return FileType.PDF;
    }

    // Récupérer un document par son ID
    public Document getDocumentById(java.util.UUID id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document non trouvé"));
    }
}
