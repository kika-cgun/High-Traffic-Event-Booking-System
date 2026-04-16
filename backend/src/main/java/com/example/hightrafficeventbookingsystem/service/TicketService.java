package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.SeatInfo;
import com.example.hightrafficeventbookingsystem.dto.TicketResponse;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ReservationEventProducer reservationEventProducer;

    @Transactional(readOnly = true)
    public List<TicketResponse> listMyTickets(Long userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdWithSeatsAndEvent(ticketId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (!ticket.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toResponse(ticket);
    }

    @Transactional
    public void confirmReservation(Long ticketId, Long userId) {
        Ticket ticket = findAndValidate(ticketId, userId);
        if (ticket.getStatus() != Status.RESERVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only RESERVED tickets can be confirmed");
        }
        ticket.setStatus(Status.CONFIRMED);
        ticketRepository.save(ticket);
        publishForAllSeats(ticket, ReservationEvent.ReservationAction.CONFIRMED);
    }

    @Transactional
    public void cancelReservation(Long ticketId, Long userId) {
        Ticket ticket = findAndValidate(ticketId, userId);
        if (ticket.getStatus() != Status.RESERVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only RESERVED tickets can be cancelled");
        }
        ticket.setStatus(Status.CANCELLED);
        for (Seat seat : ticket.getSeats()) {
            seat.setReserved(false);
            seat.setTicket(null);
        }
        ticketRepository.save(ticket);
        seatRepository.saveAll(ticket.getSeats());

        publishForAllSeats(ticket, ReservationEvent.ReservationAction.CANCELLED);
    }

    private Ticket findAndValidate(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdWithSeatsAndEvent(ticketId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (!ticket.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return ticket;
    }

    private void publishForAllSeats(Ticket ticket, ReservationEvent.ReservationAction action) {
        Instant now = Instant.now();
        for (Seat seat : ticket.getSeats()) {
            reservationEventProducer.publish(new ReservationEvent(
                ticket.getId(), ticket.getUser().getId(), seat.getId(),
                seat.getEvent().getId(), seat.getEvent().getName(), action, now
            ));
        }
    }

    private TicketResponse toResponse(Ticket ticket) {
        List<Seat> seats = ticket.getSeats();
        Event event = seats.isEmpty() ? null : seats.get(0).getEvent();

        List<SeatInfo> seatInfos = seats.stream()
            .map(s -> new SeatInfo(s.getId(), s.getSeatNumber(), s.getRowNumber(),
                s.getSection(), s.getCategory(), s.getPrice()))
            .toList();

        return new TicketResponse(
            ticket.getId(),
            ticket.getStatus(),
            ticket.getCreatedAt(),
            event != null ? event.getName() : "",
            event != null ? event.getDate() : null,
            event != null ? event.getVenueType() : null,
            seatInfos,
            ticket.getTotalPrice()
        );
    }
}
