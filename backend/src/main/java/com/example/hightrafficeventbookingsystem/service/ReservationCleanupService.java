package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {

    private static final int BATCH_SIZE = 500;

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final RedisLockService redisLockService;
    private final ReservationEventProducer reservationEventProducer;

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    @Transactional
    public void cleanup() {
        log.info("Checking for expired reservations to clean up...");

        LocalDateTime cutoffDateTime = LocalDateTime.now().minusMinutes(15);

        // PageRequest caps batch size to prevent OOM
        List<Ticket> expiredTickets = ticketRepository.findExpired(
                Status.RESERVED, cutoffDateTime, PageRequest.of(0, BATCH_SIZE));

        if (expiredTickets.isEmpty()) {
            log.info("No expired reservations found.");
            return;
        }

        log.info("Found {} expired reservations. Cleaning up...", expiredTickets.size());

        for (Ticket ticket : expiredTickets) {
            ticket.setStatus(Status.CANCELLED);

            Instant now = Instant.now();

            // For RESERVED (pending) tickets, seats are stored only in requestedSeatIds (String),
            // NOT linked via FK — so ticket.getSeats() is empty. Load them explicitly.
            // For CONFIRMED tickets (edge case), the FK collection is populated instead.
            List<Seat> seats = ticket.getSeats().isEmpty()
                    ? loadSeatsByRequestedIds(ticket.getRequestedSeatIds())
                    : ticket.getSeats();

            for (Seat seat : seats) {
                seat.setReserved(false);
                seatRepository.save(seat);

                redisLockService.releaseLock(seat.getId());

                // Broadcast CANCELLED via Kafka → WebSocket so other clients' UIs update in real time
                reservationEventProducer.publish(new ReservationEvent(
                        ticket.getId(), ticket.getUser().getId(), seat.getId(),
                        seat.getEvent().getId(), seat.getEvent().getName(),
                        ReservationEvent.ReservationAction.CANCELLED, now));

                log.info("Cancelled ticket ID {} and released seat ID {}", ticket.getId(), seat.getId());
            }
        }

        ticketRepository.saveAll(expiredTickets);
    }

    private List<Seat> loadSeatsByRequestedIds(String requestedSeatIds) {
        if (requestedSeatIds == null || requestedSeatIds.isBlank()) {
            return Collections.emptyList();
        }
        List<Long> ids = Arrays.stream(requestedSeatIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return seatRepository.findAllByIdWithEvent(ids);
    }
}
