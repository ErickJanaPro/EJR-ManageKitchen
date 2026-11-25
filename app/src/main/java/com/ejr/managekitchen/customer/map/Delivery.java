package com.ejr.managekitchen.customer.map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    protected Delivery() {
        // JPA
    }

    public Delivery(Long orderId, Long clientId, String address, Double latitude, Double longitude,
                    Double distanceKm, Integer estimatedMinutes) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.estimatedMinutes = estimatedMinutes;
        this.status = DeliveryStatus.EN_ATTENTE;
        this.createdAt = LocalDateTime.now();
    }

    public void start() {
        if (this.status == DeliveryStatus.EN_ATTENTE) {
            this.status = DeliveryStatus.EN_COURS;
            this.startedAt = LocalDateTime.now();
        }
    }

    public void complete() {
        if (this.status != DeliveryStatus.LIVREE) {
            this.status = DeliveryStatus.LIVREE;
            if (this.startedAt == null) {
                this.startedAt = LocalDateTime.now();
            }
            this.deliveredAt = LocalDateTime.now();
        }
    }

    public boolean belongsTo(Long userId) {
        return this.clientId.equals(userId);
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getClientId() { return clientId; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getDistanceKm() { return distanceKm; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public DeliveryStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
}