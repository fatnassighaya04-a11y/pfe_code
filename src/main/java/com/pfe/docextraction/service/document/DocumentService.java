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

    
    public Document uploadDocument(MultipartFile file, User user) throws IOException {

        
        byte[] fileBytes = file.getBytes();
        String checksum = computeSha256(fileBytes);

       
        Optional<Document> existing = documentRepository.findByChecksumAndDeletedAtIsNull(checksum);
        if (existing.isPresent()) {
            throw new DuplicateDocumentException(existing.get());
        }

       
        String storedFilename = fileStorageService.saveFile(file);

       
        FileType fileType = detectFileType(file.getOriginalFilename());

      
        Document document = Document
        .builder()
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

   
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    
    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

 
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

   
    public Document getDocumentById(java.util.UUID id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document non trouvé"));
    }
}
