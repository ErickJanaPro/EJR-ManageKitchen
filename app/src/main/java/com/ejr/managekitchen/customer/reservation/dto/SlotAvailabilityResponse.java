package com.ejr.managekitchen.customer.reservation.dto;

public record SlotAvailabilityResponse(
        String timeSlot,
        String service,
        int capacity,
        int reservedSeats,
        int remainingSeats,
        boolean available
) {
}