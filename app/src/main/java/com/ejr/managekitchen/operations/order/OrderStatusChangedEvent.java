package com.ejr.managekitchen.operations.order;

import com.ejr.managekitchen.operations.order.dto.OrderResponse;

import java.time.LocalDateTime;

/**
 * Événement publié à la création d'une commande ({@code previousStatus == null}) et à chaque changement de statut.
 * <p>
 * Point d'extension pour les autres domaines, sans dépendance vers le code des commandes :
 * <pre>
 * &#64;TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * public void onOrderChanged(OrderStatusChangedEvent event) { ... }
 * </pre>
 * Exemple : Dev 3 démarre le suivi de livraison quand {@code order().status() == PRETE}.
 */
public record OrderStatusChangedEvent(OrderResponse order, OrderStatus previousStatus, Long changedBy,
                                      LocalDateTime occurredAt) {

    public boolean isCreation() {
        return previousStatus == null;
    }
}
