package com.pfe.docextraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    // Token JWT retourné après login ou register
    private String token;

    // Email de l'utilisateur connecté
    private String email;

    // Rôle de l'utilisateur
    private String role;
}