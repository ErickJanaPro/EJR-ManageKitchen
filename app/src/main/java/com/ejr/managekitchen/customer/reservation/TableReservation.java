package com.ejr.managekitchen.customer.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "table_reservations")
public class TableReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "client_name", nullable = false, length = 120)
    private String clientName;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "time_slot", nullable = false, length = 10)
    private String timeSlot;

    @Column(name = "guests_count", nullable = false)
    private int guestsCount;

    @Column(name = "notes", length = 255)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMEE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TableReservation() {
    }

    public TableReservation(Long clientId,
                            String clientName,
                            LocalDate reservationDate,
                            String timeSlot,
                            int guestsCount,
                            String notes) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.reservationDate = reservationDate;
        this.timeSlot = timeSlot;
        this.guestsCount = guestsCount;
        this.notes = notes;
        this.status = ReservationStatus.CONFIRMEE;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = ReservationStatus.CONFIRMEE;
        }
    }

    public void cancel() {
        this.status = ReservationStatus.ANNULEE;
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public int getGuestsCount() {
        return guestsCount;
    }

    public String getNotes() {
        return notes;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}