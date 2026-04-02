package com.example.hightrafficeventbookingsystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(
        @NotNull(message = "Seat ID must not be null")
        @Positive(message = "Seat ID must be a positive number")
        Long seatId
) {
}
