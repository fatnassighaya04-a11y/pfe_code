package com.pfe.docextraction.service.extraction;

import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * OCR fallback pour PDFs scannés (sans couche texte) et images.
 *
 * Utilise Tesseract via tess4j. Tesseract doit être installé sur la machine
 * (ou les fichiers tessdata présents dans le chemin configuré).
 */
@Service
public class OcrService {

    @Value("${ocr.tessdata-path:./tessdata}")
    private String tessdataPath;

    @Value("${ocr.language:fra+eng}")
    private String ocrLanguage;

    @Value("${ocr.dpi:300}")
    private int renderDpi;

    private boolean ocrAvailable = false;

    @PostConstruct
    public void init() {
        File tessdataDir = new File(tessdataPath);
        if (!tessdataDir.exists() || !tessdataDir.isDirectory()) {
            System.err.println("⚠️ OCR: dossier tessdata introuvable à " + tessdataDir.getAbsolutePath()
                    + " — l'OCR sera désactivé. Téléchargez les fichiers .traineddata depuis "
                    + "https://github.com/tesseract-ocr/tessdata_fast et placez-les dans " + tessdataPath);
            return;
        }
        ocrAvailable = true;
        System.out.println("✅ OCR activé — tessdata: " + tessdataDir.getAbsolutePath() + " — langues: " + ocrLanguage);
    }

    public boolean isAvailable() {
        return ocrAvailable;
    }

    /** Effectue l'OCR sur un PDF page par page. Retourne le texte concaténé. */
    public String ocrPdf(File pdfFile) throws IOException {
        if (!ocrAvailable) return "";

        StringBuilder fullText = new StringBuilder();
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            ITesseract tesseract = createTesseract();

            int pageCount = document.getNumberOfPages();
            for (int page = 0; page < pageCount; page++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(page, renderDpi, ImageType.RGB);
                    String pageText = tesseract.doOCR(image);
                    fullText.append("[Page ").append(page + 1).append("]\n");
                    fullText.append(pageText == null ? "" : pageText.trim());
                    fullText.append("\n\n");
                } catch (Exception e) {
                    System.err.println("❌ OCR page " + (page + 1) + " : " + e.getMessage());
                }
            }
        }
        return fullText.toString().trim();
    }

    /** OCR direct sur une image (PNG, JPG...). */
    public String ocrImage(File imageFile) {
        if (!ocrAvailable) return "";
        try {
            return createTesseract().doOCR(imageFile).trim();
        } catch (Exception e) {
            System.err.println("❌ OCR image error: " + e.getMessage());
            return "";
        }
    }

    private ITesseract createTesseract() {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(ocrLanguage);
        // Mode page automatique
        tesseract.setPageSegMode(1);
        // Engine LSTM (le plus précis)
        tesseract.setOcrEngineMode(1);
        return tesseract;
    }
}
