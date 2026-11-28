package com.ejr.managekitchen.customer.reservation;

import com.ejr.managekitchen.common.exception.ConflictException;
import com.ejr.managekitchen.common.exception.NotFoundException;
import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.customer.reservation.dto.CreateReservationRequest;
import com.ejr.managekitchen.customer.reservation.dto.ReservationResponse;
import com.ejr.managekitchen.customer.reservation.dto.SlotAvailabilityResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestion des réservations de table et contrôle de capacité par créneau (EJR Kitchen — Paris Centre).
 */
@Service
@Transactional
public class ReservationService {

    public static final int MAX_CAPACITY_PER_SLOT = 40;
    public static final List<String> LUNCH_SLOTS = List.of("12:00", "12:45", "13:30");
    public static final List<String> DINNER_SLOTS = List.of("19:30", "20:30", "21:30");
    public static final List<String> ALLOWED_SLOTS = List.of("12:00", "12:45", "13:30", "19:30", "20:30", "21:30");

    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    private final TableReservationRepository reservationRepository;

    public ReservationService(TableReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<SlotAvailabilityResponse> getAvailability(LocalDate date) {
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date est obligatoire");
        }

        Map<String, Integer> reservedBySlot = reservationRepository
                .findByReservationDateAndStatus(date, ReservationStatus.CONFIRMEE)
                .stream()
                .collect(Collectors.groupingBy(
                        TableReservation::getTimeSlot,
                        Collectors.summingInt(TableReservation::getGuestsCount)
                ));

        LocalDate today = LocalDate.now(PARIS_ZONE);
        LocalTime now = LocalTime.now(PARIS_ZONE);

        return ALLOWED_SLOTS.stream().map(slot -> {
            int reserved = reservedBySlot.getOrDefault(slot, 0);
            int remaining = Math.max(0, MAX_CAPACITY_PER_SLOT - reserved);
            boolean past = isSlotInPast(date, slot, today, now);
            String service = LUNCH_SLOTS.contains(slot) ? "DEJEUNER" : "DINER";
            return new SlotAvailabilityResponse(
                    slot,
                    service,
                    MAX_CAPACITY_PER_SLOT,
                    reserved,
                    remaining,
                    !past && remaining > 0
            );
        }).toList();
    }

    public ReservationResponse createReservation(AuthenticatedUser client, CreateReservationRequest request) {
        String slot = request.timeSlot().trim();
        if (!ALLOWED_SLOTS.contains(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Créneau horaire invalide : " + slot + " (créneaux autorisés : " + String.join(", ", ALLOWED_SLOTS) + ")");
        }

        LocalDate today = LocalDate.now(PARIS_ZONE);
        LocalTime now = LocalTime.now(PARIS_ZONE);
        if (isSlotInPast(request.reservationDate(), slot, today, now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de réserver un créneau déjà passé");
        }

        int reserved = reservationRepository.sumGuestsByDateAndTimeSlotAndStatus(
                request.reservationDate(), slot, ReservationStatus.CONFIRMEE);
        int remaining = Math.max(0, MAX_CAPACITY_PER_SLOT - reserved);

        if (request.guestsCount() > remaining) {
            throw new ConflictException(
                    "Capacité insuffisante sur le créneau " + slot + " (" + remaining + " place(s) restante(s))");
        }

        String clientName = (request.clientName() != null && !request.clientName().isBlank())
                ? request.clientName().trim()
                : "Client #" + client.id();
        String notes = (request.notes() != null && !request.notes().isBlank())
                ? request.notes().trim()
                : null;

        TableReservation saved = reservationRepository.save(new TableReservation(
                client.id(),
                clientName,
                request.reservationDate(),
                slot,
                request.guestsCount(),
                notes
        ));
        return ReservationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findMyReservations(AuthenticatedUser client) {
        return reservationRepository
                .findByClientIdOrderByReservationDateDescTimeSlotDesc(client.id())
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public ReservationResponse cancelReservation(Long reservationId, AuthenticatedUser actor) {
        TableReservation reservation = (actor.hasRole(Role.CLIENT)
                ? reservationRepository.findByIdAndClientId(reservationId, actor.id())
                : reservationRepository.findById(reservationId))
                .orElseThrow(() -> new NotFoundException("Réservation introuvable : " + reservationId));

        if (reservation.getStatus() == ReservationStatus.ANNULEE) {
            throw new ConflictException("Cette réservation est déjà annulée");
        }

        reservation.cancel();
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    private static boolean isSlotInPast(LocalDate date, String slot, LocalDate today, LocalTime now) {
        if (date.isBefore(today)) {
            return true;
        }
        return date.isEqual(today) && !LocalTime.parse(slot).isAfter(now);
    }
}