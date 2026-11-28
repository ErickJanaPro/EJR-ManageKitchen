package com.ejr.managekitchen.customer.reservation;

import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.customer.reservation.dto.CreateReservationRequest;
import com.ejr.managekitchen.customer.reservation.dto.ReservationResponse;
import com.ejr.managekitchen.customer.reservation.dto.SlotAvailabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customer/reservations")
@Tag(name = "Réservations de table", description = "Disponibilités par créneau et réservations client")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/availability")
    @Operation(summary = "Consulter les créneaux et places restantes pour une date donnée")
    public List<SlotAvailabilityResponse> availability(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getAvailability(date);
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une réservation de table")
    public ReservationResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody CreateReservationRequest request) {
        return reservationService.createReservation(user, request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Lister les réservations du client connecté")
    public List<ReservationResponse> myReservations(@AuthenticationPrincipal AuthenticatedUser user) {
        return reservationService.findMyReservations(user);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Annuler une réservation de table")
    public ReservationResponse cancel(@PathVariable("id") Long id,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return reservationService.cancelReservation(id, user);
    }
}