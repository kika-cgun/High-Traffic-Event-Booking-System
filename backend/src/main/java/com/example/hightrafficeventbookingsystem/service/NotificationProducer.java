package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.config.RabbitMQConfig;
import com.example.hightrafficeventbookingsystem.dto.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendTicketNotification(TicketCreatedEvent event) {
        log.info("Sending action to RabbitMQ for ticket: {}", event);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}
