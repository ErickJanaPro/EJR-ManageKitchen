package com.ejr.managekitchen.operations.realtime;

import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.common.security.JwtService;
import com.ejr.managekitchen.common.security.Role;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Sécurité du WebSocket :
 * <ul>
 *   <li>CONNECT : JWT obligatoire, sinon la connexion est refusée</li>
 *   <li>SUBSCRIBE à /topic/kitchen/** : réservé aux rôles CUISINIER et ADMIN</li>
 * </ul>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";
    private static final String KITCHEN_TOPIC = "/topic/kitchen";

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            AuthenticatedUser user = (header != null && header.startsWith(BEARER))
                    ? jwtService.parse(header.substring(BEARER.length())).orElse(null)
                    : null;
            if (user == null) {
                throw new AccessDeniedException("Jeton JWT manquant ou invalide");
            }
            accessor.setUser(user.toAuthentication());
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(KITCHEN_TOPIC) && !isKitchenStaff(accessor.getUser())) {
                throw new AccessDeniedException("Abonnement réservé à la cuisine");
            }
        }
        return message;
    }

    private static boolean isKitchenStaff(Principal principal) {
        return principal instanceof Authentication auth
                && auth.getPrincipal() instanceof AuthenticatedUser user
                && (user.hasRole(Role.CUISINIER) || user.hasRole(Role.ADMIN));
    }
}
