package com.ejr.managekitchen.operations.order.port;

import java.util.Optional;

/**
 * Contrat : catalogue des plats, fourni par le domaine supply chain (Dev 1).
 * Tant que Dev 1 ne l'a pas implémenté, {@link StubSupplyChainAdapter} répond à sa place.
 */
public interface MenuCatalogPort {

    /** @return le plat s'il existe (disponible ou non), vide sinon. */
    Optional<DishSnapshot> findDish(Long dishId);
}
