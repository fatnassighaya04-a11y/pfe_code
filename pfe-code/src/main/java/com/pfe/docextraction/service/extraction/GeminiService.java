package com.pfe.docextraction.service.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;

@Service
public class GeminiService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${openrouter.model:google/gemini-2.0-flash-001}")
    private String model;

    @Value("${openrouter.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${openrouter.app-name:DocExtraction}")
    private String appName;

    @Value("${openrouter.site-url:http://localhost:4200}")
    private String siteUrl;

    // ================================================================
    // EXTRACTION TEXTE (PDF, TXT, DOCX)
    // ================================================================
    public String extractData(String documentText, String documentType) {
        if (!hasOpenRouterKey()) {
            return fallback(documentText, documentType);
        }

        try {
            String prompt = buildPrompt(documentText, documentType);
            String requestBody = buildTextRequest(prompt);
            return sendRequest(requestBody);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ OpenRouter error: " + e.getMessage());
            return fallback(documentText, documentType);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.err.println("❌ OpenRouter error: " + e.getMessage());
            return fallback(documentText, documentType);
        }
    }

    // ================================================================
    // EXTRACTION IMAGE (PNG, JPG, JPEG...)
    // ================================================================
    public String extractDataFromImage(File imageFile, String documentType) {
        if (!hasOpenRouterKey()) {
            return fallback("Image: " + imageFile.getName(), documentType);
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String mimeType = detectMimeType(imageFile.getName());
            return extractDataFromImage(imageBytes, mimeType, documentType);
        } catch (IOException e) {
            System.err.println("❌ OpenRouter Vision error: " + e.getMessage());
            return fallback("Image: " + imageFile.getName(), documentType);
        }
    }

    public String extractDataFromImage(byte[] imageBytes, String mimeType, String documentType) {
        if (!hasOpenRouterKey()) {
            return fallback("Image (" + mimeType + ")", documentType);
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String prompt = buildPrompt("", documentType);
            String requestBody = buildImageRequest(prompt, base64, mimeType);
            return sendRequest(requestBody);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ OpenRouter Vision error: " + e.getMessage());
            return fallback("Image (" + mimeType + ")", documentType);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.err.println("❌ OpenRouter Vision error: " + e.getMessage());
            return fallback("Image (" + mimeType + ")", documentType);
        }
    }

    // ================================================================
    // VALIDATION CLÉ API
    // ================================================================
    private boolean hasOpenRouterKey() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.equals("your_openrouter_api_key")
                && !apiKey.equals("your_api_key")
                && !apiKey.equals("REMPLACEZ_PAR_VOTRE_CLE");
    }

    // ================================================================
    // CONSTRUIRE LA REQUÊTE — TEXTE
    // ================================================================
    private String buildTextRequest(String prompt) throws IOException {
        String escapedPrompt = objectMapper.writeValueAsString(prompt);

        return """
            {
              "model": "%s",
              "temperature": 0.1,
              "max_tokens": 2048,
              "messages": [
                {
                  "role": "system",
                  "content": "Tu es un expert en extraction de données documentaires. Réponds UNIQUEMENT avec du JSON valide, sans markdown ni explication."
                },
                {
                  "role": "user",
                  "content": %s
                }
              ]
            }
            """.formatted(model, escapedPrompt);
    }

    // ================================================================
    // CONSTRUIRE LA REQUÊTE — IMAGE (Vision)
    // ================================================================
    private String buildImageRequest(String prompt,
                                      String base64Image,
                                      String mimeType) throws IOException {
        String escapedPrompt = objectMapper.writeValueAsString(prompt);

        return """
            {
              "model": "%s",
              "temperature": 0.1,
              "max_tokens": 2048,
              "messages": [
                {
                  "role": "system",
                  "content": "Tu es un expert en extraction de données documentaires. Réponds UNIQUEMENT avec du JSON valide, sans markdown ni explication."
                },
                {
                  "role": "user",
                  "content": [
                    {
                      "type": "image_url",
                      "image_url": {
                        "url": "data:%s;base64,%s"
                      }
                    },
                    {
                      "type": "text",
                      "text": %s
                    }
                  ]
                }
              ]
            }
            """.formatted(model, mimeType, base64Image, escapedPrompt);
    }

    // ================================================================
    // ENVOI HTTP
    // ================================================================
    private String sendRequest(String requestBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("HTTP-Referer", siteUrl)
            .header("X-Title", appName)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("📡 OpenRouter status: " + response.statusCode());

        return switch (response.statusCode()) {
            case 200 -> parseResponse(response.body());
            case 401 -> throw new RuntimeException("Clé API invalide (401). Vérifiez openrouter.api-key.");
            case 402 -> throw new RuntimeException("Solde insuffisant (402). Ajoutez des crédits sur openrouter.ai/credits.");
            case 429 -> throw new RuntimeException("Trop de requêtes (429). Réessayez dans quelques secondes.");
            default -> {
                System.err.println("❌ OpenRouter error body: " + response.body());
                throw new RuntimeException("OpenRouter error: " + response.statusCode());
            }
        };
    }

    // ================================================================
    // PARSER LA RÉPONSE
    // ================================================================
    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();

            // Nettoyer les markdown code fences
            content = content.trim();
            if (content.startsWith("```json")) content = content.substring(7);
            if (content.startsWith("```"))     content = content.substring(3);
            if (content.endsWith("```"))       content = content.substring(0, content.length() - 3);
            content = content.trim();

            // Extraire uniquement le JSON
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return content.substring(start, end + 1);
            }

            return content;

        } catch (IOException e) {
            System.err.println("❌ Parse error: " + e.getMessage());
            return "{\"erreur\": \"Impossible de parser la réponse OpenRouter\"}";
        }
    }

    // ================================================================
    // PROMPT SELON LE TYPE DE DOCUMENT
    // ================================================================
    private String buildPrompt(String documentText, String documentType) {
        String typeInstruction = switch (documentType) {
            case "FACTURE" -> """
                Extrais ces champs :
                numero_facture, date_emission, date_echeance,
                fournisseur, adresse_fournisseur, siret_fournisseur,
                client, adresse_client,
                montant_ht, tva_taux, montant_tva, montant_ttc,
                mode_paiement, description_prestation.
                """;
            case "BON_COMMANDE" -> """
                Extrais ces champs :
                numero_bc, date_commande, fournisseur, client,
                articles, quantites, prix_unitaires,
                montant_total, conditions_livraison.
                """;
            case "CONTRAT" -> """
                Extrais ces champs :
                parties, objet_contrat, date_debut, date_fin,
                duree, montant, conditions_paiement, clauses_importantes.
                """;
            default -> """
                Extrais TOUS les champs importants :
                montants, dates, noms, adresses, numéros, références.
                """;
        };

        String textSection = (documentText == null || documentText.isBlank())
            ? "Le document est une image — utilise la vision pour extraire les données."
            : "Texte du document :\n" + documentText.substring(0, Math.min(4000, documentText.length()));

        return """
            Tu es un expert en extraction de documents administratifs.
            Type : %s

            %s

            %s

            RÈGLES :
            - Réponds UNIQUEMENT avec un objet JSON valide.
            - Aucun texte avant ou après le JSON.
            - Si un champ est absent, mets null.
            - Montants en string ex: "1190.00"
            - Dates en format YYYY-MM-DD si possible.

            FORMAT DE RÉPONSE OBLIGATOIRE :
            {
              "extracted_data": { ...champs extraits... },
              "confidence_score": <nombre entre 0 et 100>
            }

            Le confidence_score (entre 0 et 100) doit refléter ta certitude réelle :
            - 90-100 : tous les champs lisibles et cohérents
            - 70-89  : la majorité des champs extraits, quelques-uns peu sûrs
            - 50-69  : extraction partielle, plusieurs champs manquants/illisibles
            - 0-49   : document peu lisible, beaucoup de doute
            Renvoie un VRAI nombre, pas null. Estime honnêtement.
            """.formatted(documentType, typeInstruction, textSection);
    }

    // ================================================================
    // FALLBACK SI OPENROUTER ÉCHOUE
    // ================================================================
    private String fallback(String text, String documentType) {
        String preview = (text == null ? "" : text)
            .substring(0, Math.min(100, text == null ? 0 : text.length()))
            .replace("\"", "'")
            .replace("\n", " ");

        return """
            {
              "type_document": "%s",
              "statut": "extraction_manuelle_requise",
              "apercu": "%s",
              "erreur": "Service IA temporairement indisponible"
            }
            """.formatted(documentType, preview);
    }

    // ================================================================
    // UTILITAIRES
    // ================================================================
    private String detectMimeType(String filename) {
        if (filename == null) return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        return "image/jpeg";
    }
}