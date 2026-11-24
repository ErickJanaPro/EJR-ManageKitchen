package com.ejr.managekitchen.supplychain.stock;

import com.ejr.managekitchen.supplychain.supplier.Ingredient;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_item")
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false, unique = true)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "alert_threshold", nullable = false, precision = 12, scale = 3)
    private BigDecimal alertThreshold = BigDecimal.ZERO;

    /** Verrou optimiste : deux commandes simultanées ne peuvent pas décrémenter le même stock à l'aveugle. */
    @Version
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StockItem() {
    }

    public StockItem(Ingredient ingredient) {
        this.ingredient = ingredient;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Ingredient getIngredient() { return ingredient; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAlertThreshold() { return alertThreshold; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public boolean isLow() {
        return quantity.compareTo(alertThreshold) <= 0;
    }

    /** Applique une variation (positive ou négative). Le contrôle du stock suffisant est fait par le service. */
    void apply(BigDecimal delta) {
        this.quantity = this.quantity.add(delta);
        this.updatedAt = LocalDateTime.now();
    }

    void setAlertThreshold(BigDecimal alertThreshold) {
        this.alertThreshold = alertThreshold;
        this.updatedAt = LocalDateTime.now();
    }
}
