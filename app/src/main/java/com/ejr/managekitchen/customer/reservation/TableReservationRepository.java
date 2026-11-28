package com.ejr.managekitchen.customer.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TableReservationRepository extends JpaRepository<TableReservation, Long> {

    List<TableReservation> findByClientIdOrderByReservationDateDescTimeSlotDesc(Long clientId);

    Optional<TableReservation> findByIdAndClientId(Long id, Long clientId);

    List<TableReservation> findByReservationDateAndStatus(LocalDate reservationDate, ReservationStatus status);

    @Query("""
            SELECT COALESCE(SUM(r.guestsCount), 0)
            FROM TableReservation r
            WHERE r.reservationDate = :date
              AND r.timeSlot = :timeSlot
              AND r.status = :status
            """)
    int sumGuestsByDateAndTimeSlotAndStatus(@Param("date") LocalDate date,
                                            @Param("timeSlot") String timeSlot,
                                            @Param("status") ReservationStatus status);
}