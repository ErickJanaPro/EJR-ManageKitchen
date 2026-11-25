package com.ejr.managekitchen.customer.map.dto;

import com.ejr.managekitchen.customer.map.Delivery;
import com.ejr.managekitchen.customer.map.DeliveryStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public record DeliveryResponse(
        Long id,
        Long orderId,
        String address,
        double restaurantLatitude,
        double restaurantLongitude,
        String restaurantName,
        double clientLatitude,
        double clientLongitude,
        double currentLatitude,
        double currentLongitude,
        double distanceKm,
        int estimatedMinutes,
        int progressPercent,
        DeliveryStatus status,
        LocalDateTime startedAt,
        LocalDateTime deliveredAt
) {

    public static DeliveryResponse from(Delivery d, double restLat, double restLng, String restName) {
        int progress = computeProgress(d);
        double ratio = progress / 100.0;
        double curLat = restLat + (d.getLatitude() - restLat) * ratio;
        double curLng = restLng + (d.getLongitude() - restLng) * ratio;

        return new DeliveryResponse(
                d.getId(),
                d.getOrderId(),
                d.getAddress(),
                restLat,
                restLng,
                restName,
                d.getLatitude(),
                d.getLongitude(),
                curLat,
                curLng,
                d.getDistanceKm(),
                d.getEstimatedMinutes(),
                progress,
                d.getStatus(),
                d.getStartedAt(),
                d.getDeliveredAt()
        );
    }

    private static int computeProgress(Delivery d) {
        if (d.getStatus() == DeliveryStatus.EN_ATTENTE) {
            return 0;
        }
        if (d.getStatus() == DeliveryStatus.LIVREE || d.getStartedAt() == null) {
            return 100;
        }
        long elapsedSeconds = Math.max(0, Duration.between(d.getStartedAt(), LocalDateTime.now()).getSeconds());
        return (int) Math.min(90, 15 + (elapsedSeconds * 75 / 120));
    }
}