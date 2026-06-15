
package com.pfe.docextraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;



@SpringBootApplication

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
