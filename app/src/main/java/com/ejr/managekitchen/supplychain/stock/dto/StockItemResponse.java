package com.ejr.managekitchen.supplychain.stock.dto;

import com.ejr.managekitchen.supplychain.stock.StockItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockItemResponse(
        Long ingredientId,
        String ingredientName,
        String unit,
        BigDecimal quantity,
        BigDecimal alertThreshold,
        boolean low,
        LocalDateTime updatedAt
) {
    public static StockItemResponse from(StockItem s) {
        return new StockItemResponse(s.getIngredient().getId(), s.getIngredient().getName(),
                s.getIngredient().getUnit(), s.getQuantity(), s.getAlertThreshold(), s.isLow(), s.getUpdatedAt());
    }
}
