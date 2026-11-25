package com.ejr.managekitchen.supplychain.stock.dto;

import com.ejr.managekitchen.supplychain.stock.MovementType;
import com.ejr.managekitchen.supplychain.stock.StockMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        MovementType type,
        BigDecimal delta,
        String reason,
        LocalDateTime createdAt
) {
    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(m.getId(), m.getType(), m.getDelta(), m.getReason(), m.getCreatedAt());
    }
}
