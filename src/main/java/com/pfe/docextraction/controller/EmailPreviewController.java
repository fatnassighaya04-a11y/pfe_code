package com.pfe.docextraction.controller;

import com.pfe.docextraction.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        response.put("approval", emailService.buildAccountApprovedPreviewMap());
        response.put("rejection", emailService.buildAccountRejectedPreviewMap());
        response.put("modification", emailService.buildAccountModificationPreviewMap());
        return ResponseEntity.ok(response);
    }
}