package com.ejr.managekitchen.customer.reservation.dto;

import com.ejr.managekitchen.customer.reservation.ReservationStatus;
import com.ejr.managekitchen.customer.reservation.TableReservation;

import java.time.Instant;
import java.time.LocalDate;

public record ReservationResponse(
        Long id,
        Long clientId,
        String clientName,
        LocalDate reservationDate,
        String timeSlot,
        int guestsCount,
        String notes,
        ReservationStatus status,
        Instant createdAt
) {
    public static ReservationResponse from(TableReservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getClientId(),
                r.getClientName(),
                r.getReservationDate(),
                r.getTimeSlot(),
                r.getGuestsCount(),
                r.getNotes(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}