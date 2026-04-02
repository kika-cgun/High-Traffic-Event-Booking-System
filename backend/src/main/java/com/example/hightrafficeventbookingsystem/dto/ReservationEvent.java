package com.example.hightrafficeventbookingsystem.dto;

import java.time.Instant;

/**
 * Kafka event payload emitted on every reservation state change.
 * Immutable record — serializowany do JSON przez Jackson.
 */
public record ReservationEvent(
        Long ticketId,
        Long userId,
        Long seatId,
        String eventName,
        ReservationAction action,
        Instant timestamp
) {
    public enum ReservationAction {
        RESERVED,
        CONFIRMED,
        CANCELLED
    }
}
