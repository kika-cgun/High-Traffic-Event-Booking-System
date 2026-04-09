package com.example.hightrafficeventbookingsystem.dto;

import com.example.hightrafficeventbookingsystem.model.Status;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        Status status,
        LocalDateTime createdAt,
        String eventName,
        Integer seatNumber,
        Integer rowNumber,
        String section
) {}
