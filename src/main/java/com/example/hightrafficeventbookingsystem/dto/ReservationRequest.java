package com.example.hightrafficeventbookingsystem.dto;


public record ReservationRequest(
        Long seatId,
        Long userId
) {
}
