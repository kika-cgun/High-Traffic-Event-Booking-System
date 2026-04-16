package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import com.example.hightrafficeventbookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ReservationEventProducer reservationEventProducer;

    @Transactional
    public Long reserveSeats(List<Long> seatIds, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seatIds must not be empty");
        }

        List<Long> uniqueSeatIds = seatIds.stream().distinct().toList();

        // Load all seats with their events in one query
        List<Seat> seats = seatRepository.findAllByIdWithEvent(uniqueSeatIds);
        if (seats.size() != uniqueSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more seats not found");
        }

        // All seats must belong to the same event
        Event event = seats.get(0).getEvent();
        boolean sameEvent = seats.stream().allMatch(s -> s.getEvent().getId().equals(event.getId()));
        if (!sameEvent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All seats must belong to the same event");
        }

        // Enforce per-event seat limit
        if (event.getMaxSeatsPerBooking() != null && uniqueSeatIds.size() > event.getMaxSeatsPerBooking()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot book more than " + event.getMaxSeatsPerBooking() + " seats for this event");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Seats already sold cannot be put in checkout.
        for (Seat seat : seats) {
            if (seat.isReserved()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Seat " + seat.getId() + " is already reserved");
            }
        }

        BigDecimal total = seats.stream()
                .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String requestedSeatIds = uniqueSeatIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // Reuse existing pending checkout for this event if present.
        Ticket ticket = ticketRepository.findFirstByUserIdAndEventIdAndStatusOrderByCreatedAtDesc(
                userId, event.getId(), Status.RESERVED).orElseGet(Ticket::new);

        if (ticket.getId() == null) {
            ticket.setUser(user);
            ticket.setEventId(event.getId());
            ticket.setStatus(Status.RESERVED);
        }

        ticket.setRequestedSeatIds(requestedSeatIds);
        ticket.setTotalPrice(total);
        ticketRepository.save(ticket);

        // Publish seat state changes to Kafka; WebSocket fan-out is handled by Kafka
        // consumers.
        Instant now = Instant.now();
        final Ticket savedTicket = ticket;
        seats.forEach(seat -> {
            ReservationEvent eventUpdate = new ReservationEvent(
                    savedTicket.getId(), userId, seat.getId(),
                    event.getId(), event.getName(),
                    ReservationEvent.ReservationAction.RESERVED, now);
            reservationEventProducer.publish(eventUpdate);
        });

        log.info("[Checkout] Pending ticket {} prepared for user {} — {} seat(s), awaiting Pay Now", ticket.getId(),
                userId, seats.size());
        return ticket.getId();
    }
}
