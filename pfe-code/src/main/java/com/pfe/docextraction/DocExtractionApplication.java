// ================================================================
// FICHIER : DocExtractionApplication.java
// RÔLE    : Point d'entrée principal de l'application Spring Boot
// ================================================================
package com.pfe.docextraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// @SpringBootApplication = combinaison de 3 annotations :
//   @Configuration     = cette classe peut définir des beans Spring
//   @EnableAutoConfiguration = active la configuration automatique Spring Boot
//   @ComponentScan     = scanne tous les packages pour trouver les composants

@SpringBootApplication
// @EnableAsync = permet d'utiliser @Async pour le traitement IA en arrière-plan
@EnableAsync
public class DocExtractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocExtractionApplication.class, args);
        System.out.println("""
            ================================================
            🚀 Plateforme Extraction Documents IA démarrée !
            📖 Swagger UI : http://localhost:8080/swagger-ui.html
            🔑 API Docs   : http://localhost:8080/api-docs
            ================================================
            """);
    }
}
