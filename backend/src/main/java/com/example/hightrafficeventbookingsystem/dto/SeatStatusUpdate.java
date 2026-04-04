package com.example.hightrafficeventbookingsystem.dto;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent.ReservationAction;

import java.time.Instant;

public record SeatStatusUpdate(
        Long eventId,
        Long seatId,
        Long ticketId,
        ReservationAction action,
        Instant timestamp
) {}
