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

        @Operation(summary = "Create checkout for selected seats", description = "Creates a pending checkout ticket for selected seats. Final seat reservation happens on simulated Pay Now (ticket confirm endpoint).")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Checkout created — returns pending ticket ID"),
                        @ApiResponse(responseCode = "400", description = "Invalid input — empty seatIds, seats belong to different events, or exceeds max seats per booking"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token missing or invalid"),
                        @ApiResponse(responseCode = "404", description = "One or more seats or the user not found"),
                        @ApiResponse(responseCode = "409", description = "Already reserved — seat is taken or user already has an active ticket for this event"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping
        public ResponseEntity<String> makeReservation(
                        @Valid @RequestBody ReservationRequest request,
                        Authentication authentication) {
                User currentUser = (User) authentication.getPrincipal();
                Long ticketId = reservationService.reserveSeats(request.seatIds(), currentUser.getId());
                return ResponseEntity.ok("Checkout created. Complete Pay Now to reserve seats. Ticket ID: " + ticketId);
        }
}
