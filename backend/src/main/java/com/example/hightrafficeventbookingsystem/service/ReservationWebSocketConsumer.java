package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.config.KafkaConfig;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationWebSocketConsumer {

    private final SeatWebSocketService seatWebSocketService;

    @KafkaListener(topics = KafkaConfig.RESERVATION_EVENTS_TOPIC, groupId = KafkaConfig.WS_CONSUMER_GROUP, containerFactory = "kafkaListenerContainerFactory")
    public void consumeReservationEventForWebSocket(
            @Payload ReservationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        seatWebSocketService.broadcastSeatUpdate(event);

        log.debug("[Kafka WS] ticketId={} | action={} | eventId={} | seatId={} | partition={} | offset={}",
                event.ticketId(),
                event.action(),
                event.eventId(),
                event.seatId(),
                partition,
                offset);
    }
}
