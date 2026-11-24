package com.ejr.managekitchen.supplychain.stock;

import com.ejr.managekitchen.operations.order.port.InsufficientStockException;
import com.ejr.managekitchen.supplychain.shared.ResourceNotFoundException;
import com.ejr.managekitchen.supplychain.supplier.Ingredient;
import com.ejr.managekitchen.supplychain.supplier.IngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * F2 - Gestion du stock par ingrédient.
 * <p>
 * {@link #consume} est destinée à l'implémentation de {@code StockPort} (F3) : elle s'exécute dans la
 * transaction appelante (pas de REQUIRES_NEW) pour que la commande et le stock réussissent ou échouent ensemble.
 */
@Service
@Transactional
public class StockService {

    private final StockItemRepository items;
    private final StockMovementRepository movements;
    private final IngredientRepository ingredients;

    public StockService(StockItemRepository items, StockMovementRepository movements,
                        IngredientRepository ingredients) {
        this.items = items;
        this.movements = movements;
        this.ingredients = ingredients;
    }

    /** Un ingrédient sans ligne de stock est considéré à 0 : elle est créée à la demande. */
    public List<StockItem> findAll() {
        ingredients.findAll().forEach(i -> getOrCreate(i.getId()));
        return items.findAllWithIngredient();
    }

    public List<StockItem> findLow() {
        return findAll().stream().filter(StockItem::isLow).toList();
    }

    public StockItem get(Long ingredientId) {
        return getOrCreate(ingredientId);
    }

    public List<StockMovement> movementsOf(Long ingredientId) {
        getOrCreate(ingredientId);
        return movements.findByIngredientIdOrderByCreatedAtDescIdDesc(ingredientId);
    }

    /** Entrée de marchandise. */
    public StockItem receive(Long ingredientId, BigDecimal quantity, String reason) {
        StockItem item = getOrCreate(ingredientId);
        item.apply(quantity);
        movements.save(new StockMovement(item.getIngredient(), MovementType.RECEPTION, quantity, reason));
        return item;
    }

    /** Correction d'inventaire : fixe la quantité réelle constatée. */
    public StockItem adjust(Long ingredientId, BigDecimal newQuantity, String reason) {
        StockItem item = getOrCreate(ingredientId);
        BigDecimal delta = newQuantity.subtract(item.getQuantity());
        if (delta.signum() != 0) {
            item.apply(delta);
            movements.save(new StockMovement(item.getIngredient(), MovementType.AJUSTEMENT, delta, reason));
        }
        return item;
    }

    public StockItem updateThreshold(Long ingredientId, BigDecimal threshold) {
        StockItem item = getOrCreate(ingredientId);
        item.setAlertThreshold(threshold);
        return item;
    }

    /**
     * Sortie de stock pour une commande.
     *
     * @throws InsufficientStockException si la quantité disponible est insuffisante (HTTP 409)
     */
    public StockItem consume(Long ingredientId, BigDecimal quantity, String reason) {
        StockItem item = getOrCreate(ingredientId);
        if (item.getQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException("Stock insuffisant : " + item.getIngredient().getName());
        }
        item.apply(quantity.negate());
        movements.save(new StockMovement(item.getIngredient(), MovementType.CONSOMMATION,
                quantity.negate(), reason));
        return item;
    }

    private StockItem getOrCreate(Long ingredientId) {
        return items.findByIngredientId(ingredientId).orElseGet(() -> {
            Ingredient ingredient = ingredients.findById(ingredientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrédient introuvable : " + ingredientId));
            return items.save(new StockItem(ingredient));
        });
    }
}
