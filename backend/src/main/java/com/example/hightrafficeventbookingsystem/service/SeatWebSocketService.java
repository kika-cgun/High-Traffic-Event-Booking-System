package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.dto.SeatStatusUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastSeatUpdate(ReservationEvent event) {
        SeatStatusUpdate update = new SeatStatusUpdate(
                event.eventId(),
                event.seatId(),
                event.ticketId(),
                event.action(),
                event.timestamp()
        );
        messagingTemplate.convertAndSend("/topic/seats/" + event.eventId(), update);
    }
}
