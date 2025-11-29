package com.example.hightrafficeventbookingsystem.init;

import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Role;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.repository.EventRepository;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import com.example.hightrafficeventbookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0){
            log.info("No users found in the database. Initializing default user.");

            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("password"));
            user.setEmail("email@example.com");
            user.setRole(Role.USER);
            userRepository.save(user);
            log.info("Default user created." +
                    " Username: user, Password: password");
        } else {
            log.info("Users already exist in the database. Skipping data initialization.");
        }
        if(eventRepository.count() == 0){
            log.info("No events found in the database. Initializing default events.");
            for(long i = 1L; i<=5; i++){
                Event event = new Event();
                event.setName("Event " + i);
                event.setDate(LocalDateTime.now().plusMonths(1).plusDays(i));
                eventRepository.save(event);
            }
            log.info("Default events created.");
        } else {
            log.info("Events already exist in the database. Skipping data initialization.");
        }

        if (seatRepository.count() == 0){
            log.info("No seats found in the database. Initializing default seats.");

            Event event = eventRepository.findById(1L).orElse(null);
            for (int i = 1; i <= 10; i++) {
                Seat seat = new Seat();
                seat.setSeatNumber(10 + i);
                seat.setEvent(event);
                seatRepository.save(seat);
            }

            log.info("Default seats created.");
        } else {
            log.info("Seats already exist in the database. Skipping data initialization.");
        }
    }
}
