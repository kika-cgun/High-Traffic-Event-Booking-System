package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.config.RabbitMQConfig;
import com.example.hightrafficeventbookingsystem.dto.TicketCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleTicketNotification(TicketCreatedEvent event) {
        try{
            log.info("Received ticket notification, generating PDF: {}", event.getTicketId());
            // Simulate PDF generation logic
            Thread.sleep(5000); // Simulate time taken to generate PDF
            log.info("PDF generated for ticket: {}", event.getTicketId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
