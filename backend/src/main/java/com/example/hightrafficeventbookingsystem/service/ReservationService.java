package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.dto.TicketCreatedEvent;
import com.example.hightrafficeventbookingsystem.model.*;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final RedisLockService redisLockService;
    private final NotificationProducer notificationProducer;
    private final ReservationEventProducer reservationEventProducer;

    @Transactional
    public Long reserveSeats(List<Long> seatIds, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seatIds must not be empty");
        }

        // Load all seats with their events in one query
        List<Seat> seats = seatRepository.findAllByIdWithEvent(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more seats not found");
        }

        // All seats must belong to the same event
        Event event = seats.get(0).getEvent();
        boolean sameEvent = seats.stream().allMatch(s -> s.getEvent().getId().equals(event.getId()));
        if (!sameEvent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All seats must belong to the same event");
        }

        // Enforce per-event seat limit
        if (event.getMaxSeatsPerBooking() != null && seatIds.size() > event.getMaxSeatsPerBooking()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot book more than " + event.getMaxSeatsPerBooking() + " seats for this event");
        }

        // One active ticket per user per event
        if (ticketRepository.existsActiveTicketForUserAndEvent(userId, event.getId(),
                List.of(Status.RESERVED, Status.CONFIRMED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "You already have an active reservation for this event");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Acquire Redis locks for all seats
        List<Long> locked = new ArrayList<>();
        try {
            for (Seat seat : seats) {
                boolean acquired = redisLockService.acquireLock(seat.getId(), userId);
                if (!acquired) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Seat " + seat.getId() + " is currently being reserved. Try again.");
                }
                locked.add(seat.getId());
            }

            // Verify none are already reserved (optimistic check after lock)
            for (Seat seat : seats) {
                if (seat.isReserved()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Seat " + seat.getId() + " is already reserved");
                }
            }

            // Compute total price
            BigDecimal total = seats.stream()
                .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Create ticket
            Ticket ticket = new Ticket();
            ticket.setUser(user);
            ticket.setStatus(Status.RESERVED);
            ticket.setTotalPrice(total);
            ticketRepository.save(ticket);

            // Link seats to ticket and mark reserved
            for (Seat seat : seats) {
                seat.setReserved(true);
                seat.setTicket(ticket);
                seatRepository.save(seat);
            }

            // RabbitMQ event (PDF generation + notification)
            notificationProducer.sendTicketNotification(new TicketCreatedEvent(ticket.getId(), user.getEmail()));

            // Kafka event per seat (WebSocket broadcast + audit log)
            for (Seat seat : seats) {
                ReservationEvent kafkaEvent = new ReservationEvent(
                    ticket.getId(), user.getId(), seat.getId(), event.getId(),
                    event.getName(), ReservationEvent.ReservationAction.RESERVED, Instant.now()
                );
                reservationEventProducer.publish(kafkaEvent);
            }

            log.info("[Reservation] Ticket {} created for user {} — {} seat(s)", ticket.getId(), userId, seats.size());
            return ticket.getId();

        } finally {
            locked.forEach(redisLockService::releaseLock);
        }
    }
}
