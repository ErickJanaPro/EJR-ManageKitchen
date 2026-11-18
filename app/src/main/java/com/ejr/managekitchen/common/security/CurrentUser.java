package com.ejr.managekitchen.common.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Accès à l'utilisateur connecté depuis n'importe quel service, quel que soit le domaine. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AuthenticationCredentialsNotFoundException("Aucun utilisateur authentifié");
    }
}
