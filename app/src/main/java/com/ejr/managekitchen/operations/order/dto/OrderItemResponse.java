package com.ejr.managekitchen.operations.order.dto;

import com.ejr.managekitchen.operations.order.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(Long dishId, String dishName, int quantity, BigDecimal unitPrice,
                                BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getDishId(), item.getDishName(), item.getQuantity(),
                item.getUnitPrice(), item.lineTotal());
    }
}
