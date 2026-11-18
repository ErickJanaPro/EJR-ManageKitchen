package com.ejr.managekitchen.operations.realtime;

import com.ejr.managekitchen.operations.order.OrderStatus;
import com.ejr.managekitchen.operations.order.OrderStatusChangedEvent;
import com.ejr.managekitchen.operations.order.dto.OrderResponse;

/** Message JSON envoyé sur le WebSocket. type = ORDER_CREATED ou ORDER_STATUS_CHANGED. */
public record OrderRealtimeMessage(String type, OrderStatus previousStatus, OrderResponse order) {

    public static OrderRealtimeMessage from(OrderStatusChangedEvent event) {
        String type = event.isCreation() ? "ORDER_CREATED" : "ORDER_STATUS_CHANGED";
        return new OrderRealtimeMessage(type, event.previousStatus(), event.order());
    }
}
