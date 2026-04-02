package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.config.KafkaConfig;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReservationAuditConsumer {

    /**
     * Konsumuje wszystkie eventy rezerwacji z topiku Kafki.
     *
     * Pełni rolę audit logu — każda zmiana stanu biletu (RESERVED / CONFIRMED / CANCELLED)
     * jest tutaj trwale rejestrowana. W przyszłości można podłączyć tu:
     *   - zapis do tabeli audit_log w PostgreSQL
     *   - wysyłkę do ElasticSearch / Kibana
     *   - liczniki statystyk / Prometheus metrics
     */
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

        // TODO: Persist to audit_log table or forward to analytics pipeline
    }
}
