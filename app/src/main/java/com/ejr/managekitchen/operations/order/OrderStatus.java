package com.ejr.managekitchen.operations.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * Machine à états de la commande (F6).
 * <pre>
 * NOUVELLE → EN_PREPARATION → PRETE → LIVREE     (livraison)
 *                                   → RECUPEREE  (retrait sur place)
 * </pre>
 * C'est le SEUL endroit où les transitions autorisées sont définies.
 */
public enum OrderStatus {
    NOUVELLE,
    EN_PREPARATION,
    PRETE,
    LIVREE,
    RECUPEREE;

    public boolean canTransitionTo(OrderStatus target, FulfillmentType fulfillmentType) {
        return switch (this) {
            case NOUVELLE -> target == EN_PREPARATION;
            case EN_PREPARATION -> target == PRETE;
            case PRETE -> (fulfillmentType == FulfillmentType.LIVRAISON && target == LIVREE)
                    || (fulfillmentType == FulfillmentType.RETRAIT && target == RECUPEREE);
            case LIVREE, RECUPEREE -> false;
        };
    }

    public boolean isFinal() {
        return this == LIVREE || this == RECUPEREE;
    }

    /** Statuts affichés sur l'écran de cuisine. */
    public static Set<OrderStatus> kitchenStatuses() {
        return EnumSet.of(NOUVELLE, EN_PREPARATION, PRETE);
    }

    /** Statuts comptés dans le chiffre d'affaires (commande remise au client). */
    public static Set<OrderStatus> completedStatuses() {
        return EnumSet.of(LIVREE, RECUPEREE);
    }
}
