package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.config.KafkaConfig;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.dto.ReservationEvent.ReservationAction;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaConfig.RESERVATION_EVENTS_TOPIC}
)
class KafkaReservationIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private KafkaTemplate<String, ReservationEvent> producer;
    private Consumer<String, ReservationEvent> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        JsonDeserializer<ReservationEvent> deserializer = new JsonDeserializer<>(ReservationEvent.class);
        deserializer.addTrustedPackages("com.example.hightrafficeventbookingsystem.dto");
        deserializer.setRemoveTypeHeaders(false);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-audit", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), deserializer
        ).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaConfig.RESERVATION_EVENTS_TOPIC);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void publishedReservedEvent_isAvailableOnTopic() {
        ReservationEvent event = new ReservationEvent(
                1L, 42L, 7L, 3L,
                "Spring Boot Live",
                ReservationAction.RESERVED,
                Instant.now()
        );

        producer.send(KafkaConfig.RESERVATION_EVENTS_TOPIC, event.ticketId().toString(), event);
        producer.flush();

        ConsumerRecords<String, ReservationEvent> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

        assertThat(records.count()).isEqualTo(1);
        ReservationEvent received = records.iterator().next().value();
        assertThat(received.ticketId()).isEqualTo(1L);
        assertThat(received.userId()).isEqualTo(42L);
        assertThat(received.action()).isEqualTo(ReservationAction.RESERVED);
        assertThat(received.eventName()).isEqualTo("Spring Boot Live");
    }
}
