package com.ejr.managekitchen.common.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;

/**
 * Utilisateur connecté, reconstruit à partir du JWT (aucun accès base de données).
 * <p>
 * Dans un contrôleur : {@code @AuthenticationPrincipal AuthenticatedUser user}.
 * Dans un service : {@link CurrentUser#get()}.
 * {@link #getName()} renvoie l'identifiant : c'est lui qui sert à adresser les messages WebSocket
 * vers un utilisateur précis (/user/{id}/queue/...).
 */
public record AuthenticatedUser(Long id, String email, Role role) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }

    public UsernamePasswordAuthenticationToken toAuthentication() {
        return new UsernamePasswordAuthenticationToken(this, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
