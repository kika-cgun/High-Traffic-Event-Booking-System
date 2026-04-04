package com.example.hightrafficeventbookingsystem.dto;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        LocalDateTime date,
        long availableSeats
) {}
