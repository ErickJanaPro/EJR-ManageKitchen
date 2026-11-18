package com.ejr.managekitchen.operations.notification.dto;

import com.ejr.managekitchen.operations.notification.Notification;
import com.ejr.managekitchen.operations.notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, NotificationType type, String message, Long orderId, boolean read,
                                   LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getOrderId(), n.isRead(),
                n.getCreatedAt());
    }
}
