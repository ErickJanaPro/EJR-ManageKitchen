package com.ejr.managekitchen.operations.user.dto;

import com.ejr.managekitchen.common.security.Role;
import jakarta.validation.constraints.Size;

/** Mise à jour partielle par l'administrateur : seuls les champs non nuls sont modifiés. */
public record UpdateUserRequest(@Size(max = 150) String fullName, Role role, Boolean enabled) {
}
