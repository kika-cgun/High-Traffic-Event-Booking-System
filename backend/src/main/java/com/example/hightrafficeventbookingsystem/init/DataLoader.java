package com.example.hightrafficeventbookingsystem.init;

import com.example.hightrafficeventbookingsystem.model.*;
import com.example.hightrafficeventbookingsystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("password"));
            user.setEmail("email@example.com");
            user.setRole(Role.USER);
            userRepository.save(user);
            log.info("Default user created");
        }

        if (eventRepository.count() > 0) {
            log.info("Events already exist — skipping seed");
            return;
        }

        log.info("Seeding 10 events...");

        // STADIUM events (4)
        seedStadium("UEFA Champions League Final", LocalDateTime.now().plusDays(30), 4);
        seedStadium("Formula 1 — Polish Grand Prix", LocalDateTime.now().plusDays(45), 4);
        seedStadium("Ekstraklasa — Derby Krakowa", LocalDateTime.now().plusDays(14), 4);
        seedStadium("NBA Europe Live — Lakers vs Celtics", LocalDateTime.now().plusDays(60), 6);

        // CONCERT_ARENA events (3)
        seedArena("Coldplay: Music of the Spheres Tour", LocalDateTime.now().plusDays(20), 6);
        seedArena("Metallica — 72 Seasons World Tour", LocalDateTime.now().plusDays(35), 6);
        seedArena("Taylor Swift — Eras Tour", LocalDateTime.now().plusDays(50), 6);

        // CINEMA events (3)
        seedCinema("Inception — Remaster 4K", LocalDateTime.now().plusDays(7), 8);
        seedCinema("Dune: Messiah IMAX", LocalDateTime.now().plusDays(10), 8);
        seedCinema("The Dark Knight — Screening", LocalDateTime.now().plusDays(21), 8);

        log.info("Seed complete.");
    }

    private void seedStadium(String name, LocalDateTime date, int maxSeats) {
        Event event = new Event();
        event.setName(name);
        event.setDate(date);
        event.setVenueType(VenueType.STADIUM);
        event.setMaxSeatsPerBooking(maxSeats);
        eventRepository.save(event);

        String[] sections = {"N", "S", "W", "E"};
        BigDecimal[] prices = {new BigDecimal("150"), new BigDecimal("400"), new BigDecimal("200"), new BigDecimal("200")};
        String[] categories = {"STANDARD", "VIP", "STANDARD", "STANDARD"};

        List<Seat> seats = new ArrayList<>();
        for (int si = 0; si < sections.length; si++) {
            for (int row = 1; row <= 5; row++) {
                for (int num = 1; num <= 10; num++) {
                    Seat seat = new Seat();
                    seat.setEvent(event);
                    seat.setSection(sections[si]);
                    seat.setRowNumber(row);
                    seat.setSeatNumber(num);
                    seat.setCategory(categories[si]);
                    seat.setPrice(prices[si]);
                    seat.setReserved(false);
                    seats.add(seat);
                }
            }
        }
        seatRepository.saveAll(seats);
        log.info("STADIUM '{}': {} seats created", name, seats.size());
    }

    private void seedArena(String name, LocalDateTime date, int maxSeats) {
        Event event = new Event();
        event.setName(name);
        event.setDate(date);
        event.setVenueType(VenueType.CONCERT_ARENA);
        event.setMaxSeatsPerBooking(maxSeats);
        eventRepository.save(event);

        List<Seat> seats = new ArrayList<>();

        // PIT — 100 virtual standing seats (row=0)
        for (int num = 1; num <= 100; num++) {
            Seat seat = Seat.builder()
                .event(event).section("PIT").rowNumber(0).seatNumber(num)
                .category("PIT").price(new BigDecimal("200")).reserved(false).build();
            seats.add(seat);
        }

        addSection(seats, event, "Floor L", 2, 20, "STANDARD", new BigDecimal("150"));
        addSection(seats, event, "Floor R", 2, 20, "STANDARD", new BigDecimal("150"));
        addSection(seats, event, "Main Stand", 3, 10, "PREMIUM", new BigDecimal("300"));
        addSection(seats, event, "Balcony L", 2, 15, "BALKON", new BigDecimal("80"));
        addSection(seats, event, "Balcony R", 2, 15, "BALKON", new BigDecimal("80"));
        addSection(seats, event, "Upper Stand", 2, 10, "STANDARD", new BigDecimal("100"));

        seatRepository.saveAll(seats);
        log.info("CONCERT_ARENA '{}': {} seats created", name, seats.size());
    }

    private void seedCinema(String name, LocalDateTime date, int maxSeats) {
        Event event = new Event();
        event.setName(name);
        event.setDate(date);
        event.setVenueType(VenueType.CINEMA);
        event.setMaxSeatsPerBooking(maxSeats);
        eventRepository.save(event);

        String[] sections = {"A", "B", "C", "D"};
        BigDecimal[] prices = {
            new BigDecimal("30"), new BigDecimal("40"), new BigDecimal("50"),
            new BigDecimal("50")
        };
        String[] categories = {"STANDARD", "STANDARD", "PREMIUM", "PREMIUM"};

        List<Seat> seats = new ArrayList<>();
        for (int si = 0; si < sections.length; si++) {
            for (int row = 1; row <= 5; row++) {
                for (int num = 1; num <= 10; num++) {
                    Seat seat = new Seat();
                    seat.setEvent(event);
                    seat.setSection(sections[si]);
                    seat.setRowNumber(row);
                    seat.setSeatNumber(num);
                    seat.setCategory(categories[si]);
                    seat.setPrice(prices[si]);
                    seat.setReserved(false);
                    seats.add(seat);
                }
            }
        }
        seatRepository.saveAll(seats);
        log.info("CINEMA '{}': {} seats created", name, seats.size());
    }

    private void addSection(List<Seat> seats, Event event, String section, int rows, int seatsPerRow,
                            String category, BigDecimal price) {
        for (int row = 1; row <= rows; row++) {
            for (int num = 1; num <= seatsPerRow; num++) {
                Seat seat = Seat.builder()
                    .event(event).section(section).rowNumber(row).seatNumber(num)
                    .category(category).price(price).reserved(false).build();
                seats.add(seat);
            }
        }
    }
}
