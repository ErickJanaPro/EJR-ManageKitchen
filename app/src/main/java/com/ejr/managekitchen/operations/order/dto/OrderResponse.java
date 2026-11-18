package com.ejr.managekitchen.operations.order.dto;

import com.ejr.managekitchen.operations.order.FulfillmentType;
import com.ejr.managekitchen.operations.order.Order;
import com.ejr.managekitchen.operations.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, Long clientId, OrderStatus status, FulfillmentType fulfillmentType,
                            BigDecimal total, LocalDateTime createdAt, LocalDateTime updatedAt,
                            List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getClientId(), order.getStatus(),
                order.getFulfillmentType(), order.getTotal(), order.getCreatedAt(), order.getUpdatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
