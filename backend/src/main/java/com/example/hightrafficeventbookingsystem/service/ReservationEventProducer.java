package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.config.KafkaConfig;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationEventProducer {

    private final KafkaTemplate<String, ReservationEvent> kafkaTemplate;

    /**
     * Wysyła event do topiku Kafki asynchronicznie.
     * Klucz = ticketId.toString() → gwarantuje kolejność eventów dla tego samego biletu
     * (wszystkie trafiają do tej samej partycji).
     */
    public void publish(ReservationEvent event) {
        String key = event.ticketId().toString();

        CompletableFuture<SendResult<String, ReservationEvent>> future =
                kafkaTemplate.send(KafkaConfig.RESERVATION_EVENTS_TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] Błąd wysyłania eventu dla ticketId={}: {}",
                        event.ticketId(), ex.getMessage());
            } else {
                log.info("[Kafka] Event wysłany: ticketId={}, action={}, partition={}, offset={}",
                        event.ticketId(),
                        event.action(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
