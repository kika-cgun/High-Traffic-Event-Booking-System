package com.example.hightrafficeventbookingsystem.service;

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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {

    private static final int BATCH_SIZE = 500;

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final RedisLockService redisLockService;

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    @Transactional
    public void cleanup() {
        log.info("Checking for expired reservations to clean up...");

        LocalDateTime cutoffDateTime = LocalDateTime.now().minusMinutes(15);

        // JOIN FETCH avoids N+1; PageRequest caps batch size to prevent OOM
        List<Ticket> expiredTickets = ticketRepository.findExpiredWithSeat(
                Status.RESERVED, cutoffDateTime, PageRequest.of(0, BATCH_SIZE));

        if (expiredTickets.isEmpty()) {
            log.info("No expired reservations found.");
            return;
        }

        log.info("Found {} expired reservations. Cleaning up...", expiredTickets.size());

        for (Ticket ticket : expiredTickets) {
            ticket.setStatus(Status.CANCELLED);

            Seat seat = ticket.getSeat(); // already fetched via JOIN FETCH — no extra query
            seat.setReserved(false);
            seatRepository.save(seat);

            redisLockService.releaseLock(seat.getId());

            log.info("Cancelled ticket ID {} and released seat ID {}", ticket.getId(), seat.getId());
        }

        ticketRepository.saveAll(expiredTickets);
    }
}
