package com.pfe.docextraction.service;

import com.pfe.docextraction.entity.ApiKey;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKey generateKey(String appName, User createdBy, int expiresInDays) {
        String rawKey = generateRandomKey();
        ApiKey apiKey = ApiKey.builder()
                .keyValue(rawKey)
                .appName(appName)
                .createdBy(createdBy)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(expiresInDays))
                .build();
        return apiKeyRepository.save(apiKey);
    }

    public void revokeKey(UUID id) {
        ApiKey key = apiKeyRepository.findById(id).orElseThrow(() -> new RuntimeException("Clé non trouvée"));
        key.setIsActive(false);
        apiKeyRepository.save(key);
    }

    private String generateRandomKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "pk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}