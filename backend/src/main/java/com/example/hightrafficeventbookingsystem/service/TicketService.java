package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.SeatInfo;
import com.example.hightrafficeventbookingsystem.dto.TicketResponse;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.dto.TicketCreatedEvent;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final RedisLockService redisLockService;
    private final NotificationProducer notificationProducer;
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

        List<Long> requestedSeatIds = parseRequestedSeatIds(ticket.getRequestedSeatIds());
        if (requestedSeatIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No seats selected for this ticket");
        }

        List<Seat> seats = seatRepository.findAllByIdWithEvent(requestedSeatIds);
        if (seats.size() != requestedSeatIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more seats not found");
        }

        Event event = seats.get(0).getEvent();
        boolean sameEvent = seats.stream().allMatch(s -> s.getEvent().getId().equals(event.getId()));
        if (!sameEvent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All seats must belong to the same event");
        }

        if (ticket.getEventId() != null && !ticket.getEventId().equals(event.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket event does not match selected seats");
        }

        seats = seats.stream().sorted(java.util.Comparator.comparing(Seat::getId)).toList();

        List<Long> locked = new ArrayList<>();
        try {
            for (Seat seat : seats) {
                boolean acquired = redisLockService.acquireLock(seat.getId(), userId);
                if (!acquired) {
                    throw new ResponseStatusException(HttpStatus.LOCKED,
                            "Seat " + seat.getId() + " is currently being reserved. Try again.");
                }
                locked.add(seat.getId());
            }

            for (Seat seat : seats) {
                if (seat.isReserved()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Seat " + seat.getId() + " is already reserved");
                }
            }

            for (Seat seat : seats) {
                seat.setReserved(true);
                seat.setTicket(ticket);
            }
            seatRepository.saveAll(seats);

            ticket.setStatus(Status.CONFIRMED);
            ticketRepository.save(ticket);

            notificationProducer
                    .sendTicketNotification(new TicketCreatedEvent(ticket.getId(), ticket.getUser().getEmail()));
            publishForSeats(ticket, seats, ReservationEvent.ReservationAction.CONFIRMED);
        } finally {
            locked.forEach(redisLockService::releaseLock);
        }
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

        publishForSeats(ticket, ticket.getSeats(), ReservationEvent.ReservationAction.CANCELLED);
    }

    private Ticket findAndValidate(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdWithSeatsAndEvent(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (!ticket.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return ticket;
    }

    private void publishForSeats(Ticket ticket, List<Seat> seats, ReservationEvent.ReservationAction action) {
        Instant now = Instant.now();
        for (Seat seat : seats) {
            reservationEventProducer.publish(new ReservationEvent(
                    ticket.getId(), ticket.getUser().getId(), seat.getId(),
                    seat.getEvent().getId(), seat.getEvent().getName(), action, now));
        }
    }

    private List<Long> parseRequestedSeatIds(String requestedSeatIdsRaw) {
        if (requestedSeatIdsRaw == null || requestedSeatIdsRaw.isBlank()) {
            return List.of();
        }

        try {
            return java.util.Arrays.stream(requestedSeatIdsRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .distinct()
                    .toList();
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid seat IDs in ticket", ex);
        }
    }

    private TicketResponse toResponse(Ticket ticket) {
        List<Seat> seats = ticket.getSeats();
        if (seats.isEmpty()) {
            List<Long> requestedSeatIds = parseRequestedSeatIds(ticket.getRequestedSeatIds());
            if (!requestedSeatIds.isEmpty()) {
                seats = seatRepository.findAllByIdWithEvent(requestedSeatIds);
            }
        }

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
                ticket.getTotalPrice());
    }
}
