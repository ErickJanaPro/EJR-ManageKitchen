package com.ejr.managekitchen.operations.user.dto;

import com.ejr.managekitchen.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Création d'un compte par l'administrateur (ex. un cuisinier). */
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "8 caractères minimum") String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role) {
}
