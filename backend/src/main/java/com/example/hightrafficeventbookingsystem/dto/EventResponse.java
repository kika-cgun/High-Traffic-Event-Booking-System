package com.example.hightrafficeventbookingsystem.dto;

import com.example.hightrafficeventbookingsystem.model.VenueType;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        LocalDateTime date,
        VenueType venueType,
        Integer maxSeatsPerBooking,
        String ticketLimitMessage,
        long availableSeats) {
}
