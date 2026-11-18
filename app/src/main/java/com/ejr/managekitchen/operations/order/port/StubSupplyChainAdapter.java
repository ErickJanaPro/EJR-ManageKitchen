package com.ejr.managekitchen.operations.order.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simulation TEMPORAIRE du domaine supply chain, pour développer et tester les commandes sans attendre Dev 1.
 * <p>
 * Désactivation : quand Dev 1 fournit ses propres implémentations de {@link MenuCatalogPort} et {@link StockPort},
 * passer {@code app.stubs.supplychain=false} dans application.yml.
 */
@Component
@ConditionalOnProperty(prefix = "app.stubs", name = "supplychain", havingValue = "true", matchIfMissing = true)
public class StubSupplyChainAdapter implements MenuCatalogPort, StockPort {

    private static final Logger log = LoggerFactory.getLogger(StubSupplyChainAdapter.class);

    private static final Map<Long, DishSnapshot> DISHES = Map.of(
            1L, new DishSnapshot(1L, "Burger végétarien", new BigDecimal("12.50"), true),
            2L, new DishSnapshot(2L, "Salade de saison", new BigDecimal("9.00"), true),
            3L, new DishSnapshot(3L, "Tiramisu", new BigDecimal("6.50"), true),
            4L, new DishSnapshot(4L, "Risotto aux cèpes", new BigDecimal("15.00"), false));

    @Override
    public Optional<DishSnapshot> findDish(Long dishId) {
        return Optional.ofNullable(DISHES.get(dishId));
    }

    @Override
    public void consumeIngredientsFor(List<OrderLine> lines) {
        log.info("[STUB] Décrémentation du stock simulée pour {}", lines);
    }
}
