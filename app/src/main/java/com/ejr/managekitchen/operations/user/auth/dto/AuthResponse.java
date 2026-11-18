package com.ejr.managekitchen.operations.user.auth.dto;

import com.ejr.managekitchen.operations.user.dto.UserResponse;

/** Réponse de connexion : le front envoie ensuite « Authorization: Bearer {accessToken} ». */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
