package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.repository.EventRepository;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.UserRepository;
import com.example.hightrafficeventbookingsystem.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class ReservationConcurrencyTest {

    // 1. PostgreSQL
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    // 2. RabbitMQ
    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    // 3. REDIS
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:alpine"))
            .withExposedPorts(6379);

    // Konfiguracja dynamiczna dla Redisa (Spring musi wiedzieć na jakim losowym porcie wstał Redis)
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;

    private Long seatId;
    private List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Czyścimy bazę
        seatRepository.deleteAll();
        userRepository.deleteAll();
        eventRepository.deleteAll();
        userIds.clear();

        // Tworzymy Event
        Event event = new Event();
        event.setName("Test Event");
        event.setDate(LocalDateTime.now().plusDays(10));
        eventRepository.save(event);

        // Tworzymy Miejsce
        Seat seat = new Seat();
        seat.setSeatNumber(10);
        seat.setRowNumber(1);
        seat.setSection("VIP");
        seat.setEvent(event);
        seat.setVersion(0);
        seat.setReserved(false);

        Seat savedSeat = seatRepository.save(seat);
        seatId = savedSeat.getId();

        // Tworzymy 10 użytkowników
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setEmail("user" + i + "@test.com");
            user.setUsername("user" + i);
            user.setName("Jan");
            // user.setPassword(...) // jeśli wymagane
            // user.setRole(Role.USER); // jeśli wymagane
            user = userRepository.save(user);
            userIds.add(user.getId());
        }
    }

    @Test
    void shouldSellSeatOnlyOnce_WhenMultipleUsersTryToBuyIt() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final Long userId = userIds.get(i);

            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    reservationService.reserveSeat(seatId, userId);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    e.printStackTrace();
                    failCount.incrementAndGet();
                }
            });
        }

        Thread.sleep(2000);

        System.out.println("Successful reservations: " + successCount.get());
        System.out.println("Cancelled reservations: " + failCount.get());

        // Asercja
        assertEquals(1, successCount.get(), "Just one reservation should succeed.");
        assertEquals(threadCount - 1, failCount.get(), "The rest should fail.");
    }
}