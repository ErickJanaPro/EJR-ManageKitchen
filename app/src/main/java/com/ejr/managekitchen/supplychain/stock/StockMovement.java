package com.ejr.managekitchen.supplychain.stock;

import com.ejr.managekitchen.supplychain.supplier.Ingredient;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Historique immuable de chaque entrée / sortie de stock. */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    /** Positif pour une entrée, négatif pour une sortie. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal delta;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockMovement() {
    }

    public StockMovement(Ingredient ingredient, MovementType type, BigDecimal delta, String reason) {
        this.ingredient = ingredient;
        this.type = type;
        this.delta = delta;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Ingredient getIngredient() { return ingredient; }
    public MovementType getType() { return type; }
    public BigDecimal getDelta() { return delta; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
