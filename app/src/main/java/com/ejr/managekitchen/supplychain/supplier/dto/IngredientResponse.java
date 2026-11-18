package com.ejr.managekitchen.supplychain.supplier.dto;

import com.ejr.managekitchen.supplychain.supplier.Ingredient;

import java.math.BigDecimal;

public record IngredientResponse(
        Long id,
        String name,
        String unit,
        BigDecimal unitPrice,
        Long supplierId
) {
    public static IngredientResponse from(Ingredient i) {
        return new IngredientResponse(i.getId(), i.getName(), i.getUnit(),
                i.getUnitPrice(), i.getSupplier().getId());
    }
}
