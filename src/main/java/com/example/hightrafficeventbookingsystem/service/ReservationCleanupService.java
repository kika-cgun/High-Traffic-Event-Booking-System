package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final RedisLockService redisLockService;

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    @Transactional
    public void cleanup() {
        log.info("Checking for expired reservations to clean up...");

        //For testing
        //LocalDateTime cutoffDateTime = LocalDateTime.now().minusSeconds(15);

        LocalDateTime cutoffDateTime = LocalDateTime.now().minusMinutes(10);

        List<Ticket> expiredTickets = ticketRepository.findByStatusAndCreatedAtBefore(Status.RESERVED, cutoffDateTime);

        if(expiredTickets.isEmpty()) {
            log.info("No expired reservations found.");
            return;
        }

        log.info("Found {} expired reservations. Cleaning up...", expiredTickets.size());

        for(Ticket ticket : expiredTickets) {
            ticket.setStatus(Status.CANCELLED);

            Seat seat = ticket.getSeat();
            seat.setReserved(false);
            seatRepository.save(seat);

            redisLockService.releaseLock(seat.getId());

            log.info("Cancelled ticket ID {} and released seat ID {}", ticket.getId(), seat.getId());
        }

        ticketRepository.saveAll(expiredTickets);
    }
}
