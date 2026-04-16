package com.example.hightrafficeventbookingsystem.dto;

import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.VenueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
    Long id,
    Status status,
    LocalDateTime createdAt,
    String eventName,
    LocalDateTime eventDate,
    VenueType venueType,
    List<SeatInfo> seats,
    BigDecimal totalPrice
) {}
