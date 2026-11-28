package com.ejr.managekitchen.customer.reservation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateReservationRequest(
        @NotNull(message = "La date de réservation est obligatoire")
        @FutureOrPresent(message = "La date de réservation ne peut pas être dans le passé")
        LocalDate reservationDate,

        @NotBlank(message = "Le créneau horaire est obligatoire")
        @Size(max = 10, message = "Créneau horaire invalide")
        String timeSlot,

        @Min(value = 1, message = "Il faut au moins 1 couvert")
        @Max(value = 12, message = "Maximum 12 couverts par réservation en ligne")
        int guestsCount,

        @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères")
        String clientName,

        @Size(max = 255, message = "La remarque ne peut pas dépasser 255 caractères")
        String notes
) {
}