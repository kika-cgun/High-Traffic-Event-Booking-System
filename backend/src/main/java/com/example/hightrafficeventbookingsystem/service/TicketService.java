package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.dto.TicketResponse;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ReservationEventProducer reservationEventProducer;

    public List<TicketResponse> listMyTickets(Long userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void confirmReservation(Long ticketId, Long userId) {
        Ticket ticket = findTicket(ticketId);
        validateOwnership(ticket, userId);
        if (ticket.getStatus() != Status.RESERVED) {
            throw new IllegalStateException("Only RESERVED tickets can be confirmed");
        }
        ticket.setStatus(Status.CONFIRMED);
        ticketRepository.save(ticket);
        reservationEventProducer.publish(buildEvent(ticket, ReservationEvent.ReservationAction.CONFIRMED));
    }

    @Transactional
    public void cancelReservation(Long ticketId, Long userId) {
        Ticket ticket = findTicket(ticketId);
        validateOwnership(ticket, userId);
        if (ticket.getStatus() != Status.RESERVED) {
            throw new IllegalStateException("Only RESERVED tickets can be cancelled");
        }
        ticket.setStatus(Status.CANCELLED);
        Seat seat = ticket.getSeat();
        seat.setReserved(false);
        seatRepository.save(seat);
        ticketRepository.save(ticket);
        reservationEventProducer.publish(buildEvent(ticket, ReservationEvent.ReservationAction.CANCELLED));
    }

    private Ticket findTicket(Long ticketId) {
        return ticketRepository.findByIdWithSeatAndEvent(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    private void validateOwnership(Ticket ticket, Long userId) {
        if (!ticket.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Ticket does not belong to this user");
        }
    }

    private ReservationEvent buildEvent(Ticket ticket, ReservationEvent.ReservationAction action) {
        Seat seat = ticket.getSeat();
        return new ReservationEvent(
                ticket.getId(),
                ticket.getUser().getId(),
                seat.getId(),
                seat.getEvent().getId(),
                seat.getEvent().getName(),
                action,
                Instant.now()
        );
    }

    private TicketResponse toResponse(Ticket ticket) {
        Seat seat = ticket.getSeat();
        return new TicketResponse(
                ticket.getId(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                seat.getEvent().getName(),
                seat.getSeatNumber(),
                seat.getRowNumber(),
                seat.getSection()
        );
    }
}
