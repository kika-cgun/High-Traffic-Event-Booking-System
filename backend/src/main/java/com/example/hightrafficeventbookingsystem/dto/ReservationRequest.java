package com.example.hightrafficeventbookingsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReservationRequest(
    @NotEmpty(message = "seatIds must not be empty")
    List<Long> seatIds
) {}
