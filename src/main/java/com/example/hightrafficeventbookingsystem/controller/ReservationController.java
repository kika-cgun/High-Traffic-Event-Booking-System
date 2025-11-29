package com.example.hightrafficeventbookingsystem.controller;

import com.example.hightrafficeventbookingsystem.dto.ReservationRequest;
import com.example.hightrafficeventbookingsystem.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

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
