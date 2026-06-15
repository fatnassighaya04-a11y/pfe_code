package com.pfe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReclamationDTO {
    private Long id;
    private Long userId;
    private String type;
    private String subject;
    private String description;
    private String priority;
    private String status;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
