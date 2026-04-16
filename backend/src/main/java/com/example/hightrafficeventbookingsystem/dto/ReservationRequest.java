package com.example.hightrafficeventbookingsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReservationRequest(
    @NotNull(message = "seatIds must not be null")
    @NotEmpty(message = "seatIds must not be empty")
    List<@NotNull Long> seatIds
) {}
