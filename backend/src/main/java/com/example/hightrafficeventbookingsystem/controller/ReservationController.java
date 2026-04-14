package com.example.hightrafficeventbookingsystem.controller;

import com.example.hightrafficeventbookingsystem.dto.ReservationRequest;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Controller", description = "Endpoints for managing reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(
            summary = "Reserve one or more seats for an event",
            description = "Reserves the selected seats (multi-seat) for the authenticated user. All seats must belong to the same event. Uses Redis distributed locking per seat and enforces per-event booking limits."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation successful — returns ticket ID"),
            @ApiResponse(responseCode = "400", description = "Invalid input — empty seatIds, seats belong to different events, or exceeds max seats per booking"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "One or more seats or the user not found"),
            @ApiResponse(responseCode = "409", description = "Already reserved — seat is taken or user already has an active ticket for this event"),
            @ApiResponse(responseCode = "423", description = "Seat locked — seat is currently being reserved by another request, try again"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<String> makeReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Long ticketId = reservationService.reserveSeats(request.seatIds(), currentUser.getId());
        return ResponseEntity.ok("Reservation successful. Ticket ID: " + ticketId);
    }
}
