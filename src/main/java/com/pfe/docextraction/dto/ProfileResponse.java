package com.pfe.docextraction.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String telephone;
    private String role;
    private String accountStatus;
    private LocalDateTime createdAt;
    
    public String getUsername() {
        return username;
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    public String getEmail() {
        return email;
    }
}