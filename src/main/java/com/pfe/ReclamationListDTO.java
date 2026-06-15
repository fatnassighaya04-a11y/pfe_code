package com.pfe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReclamationListDTO {
    private Long id;
    private String subject;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
}
