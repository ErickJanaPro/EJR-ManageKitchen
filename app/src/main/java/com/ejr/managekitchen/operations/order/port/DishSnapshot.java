package com.ejr.managekitchen.operations.order.port;

import java.math.BigDecimal;

/** Ce que le module Commandes a besoin de savoir d'un plat au moment de la commande. */
public record DishSnapshot(Long id, String name, BigDecimal price, boolean available) {
}
