package com.example.hightrafficeventbookingsystem.controller;

import com.example.hightrafficeventbookingsystem.dto.TicketResponse;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.service.PdfTicketService;
import com.example.hightrafficeventbookingsystem.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket Controller", description = "Endpoints for managing user tickets")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;
    private final PdfTicketService pdfTicketService;

    @Operation(summary = "Get ticket by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ticketService.getTicketById(id, user.getId()));
    }

    @Operation(summary = "Get my tickets")
    @GetMapping("/my")
    public ResponseEntity<List<TicketResponse>> listMyTickets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ticketService.listMyTickets(user.getId()));
    }

    @Operation(summary = "Simulate Pay Now and confirm reservation")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmReservation(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ticketService.confirmReservation(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel reservation")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ticketService.cancelReservation(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Download ticket as PDF")
    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        byte[] pdf = pdfTicketService.generatePdf(id, user.getId());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"bilet-" + id + ".pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }
}
