package com.example.hightrafficeventbookingsystem.model;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long seatId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationEvent.ReservationAction action;

    @Column(nullable = false)
    private Instant occurredAt;

    public AuditLog(ReservationEvent event) {
        this.ticketId = event.ticketId();
        this.userId = event.userId();
        this.seatId = event.seatId();
        this.eventId = event.eventId();
        this.eventName = event.eventName();
        this.action = event.action();
        this.occurredAt = event.timestamp();
    }
}
