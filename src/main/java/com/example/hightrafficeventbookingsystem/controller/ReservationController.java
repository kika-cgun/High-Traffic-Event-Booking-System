package com.example.hightrafficeventbookingsystem.controller;

import com.example.hightrafficeventbookingsystem.dto.ReservationRequest;
import com.example.hightrafficeventbookingsystem.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Controller", description = "Endpoints for managing reservations")
public class ReservationController {
    private final ReservationService reservationService;

    @Operation(summary = "Make a reservation for a seat", description = "Attempts to reserve the selected seat for the user. Supports Redis locks and Optimistic Locking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation successful"),
            @ApiResponse(responseCode = "409", description = "Seat is already reserved"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> makeReservation(@RequestBody ReservationRequest request) {
        try{
            Long ticketId = reservationService.reserveSeat(request.seatId(), request.userId());

            return ResponseEntity.ok("Reservation successful. Ticket ID: " + ticketId);
        } catch (RuntimeException e){
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}
