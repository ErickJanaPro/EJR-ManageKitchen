package com.ejr.managekitchen.operations.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Notification conservée en base : le client la retrouve même s'il n'était pas connecté au moment de l'envoi. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
        // JPA
    }

    public Notification(Long recipientId, NotificationType type, String message, Long orderId) {
        this.recipientId = recipientId;
        this.type = type;
        this.message = message;
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() { return id; }
    public Long getRecipientId() { return recipientId; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public Long getOrderId() { return orderId; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
