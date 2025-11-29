package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.TicketCreatedEvent;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import com.example.hightrafficeventbookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final RedisLockService redisLockService;
    private final NotificationProducer notificationProducer;

    @Transactional
    public Long reserveSeat(Long seatId, Long userId) {

        boolean locked = redisLockService.acquireLock(seatId, userId);
        if (!locked) {
            throw new IllegalStateException("Seat is being reserved by another user. Please try again.");
        }

        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
            Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new IllegalArgumentException("Seat not found"));

            if (seat.isReserved()) {
                throw new IllegalStateException("Seat is already reserved");
            }

            seat.setReserved(true);
            seatRepository.save(seat);

            Ticket ticket = new Ticket();
            ticket.setSeat(seat);
            ticket.setUser(user);
            ticket.setStatus(Status.RESERVED);
            ticketRepository.save(ticket);

            TicketCreatedEvent event = new TicketCreatedEvent(
                    ticket.getId(),
                    user.getEmail()
            );

            notificationProducer.sendTicketNotification(event);

            return ticket.getId();
        } catch (Exception e) {
            redisLockService.releaseLock(seatId);
            throw e;
        }
    }
}
