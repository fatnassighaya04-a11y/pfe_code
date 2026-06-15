package com.pfe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReclamationDTO {
    
    @NotBlank(message = "Type cannot be blank")
    private String type;
    
    @NotBlank(message = "Subject cannot be blank")
    @Size(min = 5, max = 200, message = "Subject must be between 5 and 200 characters")
    private String subject;
    
    @NotBlank(message = "Description cannot be blank")
    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    private String description;
    
    @NotNull(message = "Priority cannot be null")
    private String priority;
    
    private String fileName;
    
    public void setPriorityDefault() {
        if (this.priority == null || this.priority.isBlank()) {
            this.priority = "MEDIUM";
        }
    }
}
