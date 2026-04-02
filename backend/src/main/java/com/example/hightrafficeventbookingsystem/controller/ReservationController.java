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
            summary = "Make a reservation for a seat",
            description = "Reserves the selected seat for the authenticated user. Uses Redis distributed lock and Optimistic Locking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Seat or user not found"),
            @ApiResponse(responseCode = "409", description = "Seat is already reserved"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<String> makeReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Long ticketId = reservationService.reserveSeat(request.seatId(), currentUser.getId());
        return ResponseEntity.ok("Reservation successful. Ticket ID: " + ticketId);
    }
}
