package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.EventResponse;
import com.example.hightrafficeventbookingsystem.dto.SeatResponse;
import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.repository.EventRepository;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    public Page<EventResponse> listEvents(Pageable pageable) {
        return eventRepository.findAll(pageable).map(event -> {
            long available = seatRepository.findByEventId(event.getId())
                    .stream().filter(s -> !s.isReserved()).count();
            return new EventResponse(event.getId(), event.getName(), event.getDate(),
                    event.getVenueType(), event.getMaxSeatsPerBooking(), buildTicketLimitMessage(event), available);
        });
    }

    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        long available = seatRepository.findByEventId(eventId).stream()
                .filter(s -> !s.isReserved()).count();
        return new EventResponse(event.getId(), event.getName(), event.getDate(),
                event.getVenueType(), event.getMaxSeatsPerBooking(), buildTicketLimitMessage(event), available);
    }

    public List<SeatResponse> listSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        return seatRepository.findByEventId(eventId).stream()
                .map(this::toSeatResponse)
                .toList();
    }

    private SeatResponse toSeatResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(), seat.getSeatNumber(), seat.getRowNumber(),
                seat.getSection(), seat.getCategory(), seat.getPrice(), seat.isReserved());
    }

    private String buildTicketLimitMessage(Event event) {
        Integer limit = event.getMaxSeatsPerBooking();
        if (limit == null || limit <= 0) {
            return null;
        }
        return "Ograniczenie biletowe: maksymalnie " + limit + " na osobe";
    }
}
