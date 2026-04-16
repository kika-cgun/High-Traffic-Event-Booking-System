package com.example.hightrafficeventbookingsystem.dto;

import java.math.BigDecimal;

public record SeatInfo(
    Long id,
    Integer seatNumber,
    Integer rowNumber,
    String section,
    String category,
    BigDecimal price
) {}
