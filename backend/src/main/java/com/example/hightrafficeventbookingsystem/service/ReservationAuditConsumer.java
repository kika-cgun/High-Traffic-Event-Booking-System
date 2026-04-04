package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.config.KafkaConfig;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.model.AuditLog;
import com.example.hightrafficeventbookingsystem.repository.AuditLogRepository;
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
public class ReservationAuditConsumer {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(
            topics = KafkaConfig.RESERVATION_EVENTS_TOPIC,
            groupId = KafkaConfig.AUDIT_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeReservationEvent(
            @Payload ReservationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("[Kafka AUDIT] ticketId={} | userId={} | action={} | event='{}' | partition={} | offset={} | timestamp={}",
                event.ticketId(),
                event.userId(),
                event.action(),
                event.eventName(),
                partition,
                offset,
                event.timestamp()
        );

        auditLogRepository.save(new AuditLog(event));
    }
}
