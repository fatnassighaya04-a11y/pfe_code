package com.pfe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateResponseDTO {
    
    @NotBlank(message = "Response text cannot be blank")
    @Size(min = 10, max = 5000, message = "Response must be between 10 and 5000 characters")
    private String responseText;
}
