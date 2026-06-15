package com.pfe.docextraction.controller;

import com.pfe.docextraction.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailPreviewController {

    private final EmailService emailService;

    @GetMapping("/previews")
    public ResponseEntity<Map<String, Object>> getPreviews() {
        Map<String, Object> response = new HashMap<>();
        response.put("approval", emailService.buildAccountApprovedPreview());
        response.put("rejection", emailService.buildAccountRejectedPreview());
        response.put("modification", emailService.buildAccountModificationPreview());
        return ResponseEntity.ok(response);
    }
}