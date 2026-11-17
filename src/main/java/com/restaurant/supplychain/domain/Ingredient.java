package com.restaurant.supplychain.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    protected Ingredient() {
    }

    public Ingredient(String name, String unit, BigDecimal unitPrice, Supplier supplier) {
        this.name = name;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.supplier = supplier;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Supplier getSupplier() { return supplier; }
}
