package com.ejr.managekitchen.operations.user.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inscription publique d'un client. */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "8 caractères minimum") String password,
        @NotBlank @Size(max = 150) String fullName) {
}
