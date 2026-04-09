package com.example.hightrafficeventbookingsystem.dto;

public record SeatResponse(
        Long id,
        Integer seatNumber,
        Integer rowNumber,
        String section,
        boolean reserved
) {}
