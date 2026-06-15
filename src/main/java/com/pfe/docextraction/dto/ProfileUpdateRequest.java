package com.pfe.docextraction.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String username;
    
    @Email(message = "Email invalide")
    private String email;
    
    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String telephone;
    
    // Getters explicites (au cas où Lombok ne fonctionne pas)
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getTelephone() {
        return telephone;
    }
    
    // Setters explicites
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}