package com.pfe.docextraction.service.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    // Lit le dossier de stockage depuis application.properties
    @Value("${file.storage.location}")
    private String storageLocation;

    // Sauvegarde le fichier sur le disque
    public String saveFile(MultipartFile file) throws IOException {

        // Créer le dossier s'il n'existe pas
        Path uploadPath = Paths.get(storageLocation);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Générer un nom unique avec UUID
        // Ex: "550e8400-e29b-41d4-a716.pdf"
        String extension = getExtension(file.getOriginalFilename());
        String newFilename = UUID.randomUUID().toString() + extension;

        // Sauvegarder le fichier
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        return newFilename;
    }

    // Extraire l'extension du fichier
    // Ex: "facture.pdf" → ".pdf"
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}