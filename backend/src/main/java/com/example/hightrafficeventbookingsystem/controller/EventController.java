package com.example.hightrafficeventbookingsystem.controller;

import com.example.hightrafficeventbookingsystem.dto.EventResponse;
import com.example.hightrafficeventbookingsystem.dto.SeatResponse;
import com.example.hightrafficeventbookingsystem.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Event Controller", description = "Public endpoints for browsing events and seats")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "List all events", description = "Paginated list of events. Public — no auth required.")
    @GetMapping
    public ResponseEntity<Page<EventResponse>> listEvents(
            @PageableDefault(size = 20, sort = "date") Pageable pageable) {
        return ResponseEntity.ok(eventService.listEvents(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @Operation(summary = "List seats for an event", description = "Returns all seats with availability status. Public — no auth required.")
    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatResponse>> listSeats(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.listSeats(id));
    }
}
