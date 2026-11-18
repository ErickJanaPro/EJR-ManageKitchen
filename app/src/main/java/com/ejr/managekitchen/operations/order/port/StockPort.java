package com.ejr.managekitchen.operations.order.port;

import java.util.List;

/**
 * Contrat : décrémentation automatique du stock, fournie par le domaine supply chain (Dev 1).
 * <p>
 * Appelée de façon SYNCHRONE, dans la même transaction que la création de la commande
 * (étape 3 du scénario nominal). Si le stock est insuffisant, l'implémentation lève
 * {@link InsufficientStockException} : la transaction est annulée et aucune commande n'est créée.
 */
public interface StockPort {

    void consumeIngredientsFor(List<OrderLine> lines);
}
