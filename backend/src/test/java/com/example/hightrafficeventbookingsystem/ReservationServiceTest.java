package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import com.example.hightrafficeventbookingsystem.repository.UserRepository;
import com.example.hightrafficeventbookingsystem.service.ReservationEventProducer;
import com.example.hightrafficeventbookingsystem.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    SeatRepository seatRepository;
    @Mock
    TicketRepository ticketRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ReservationEventProducer reservationEventProducer;

    @InjectMocks
    ReservationService reservationService;

    @Test
    void reserveSeats_publishesReservedKafkaEventForEveryUniqueSeat() {
        Event event = new Event();
        event.setId(5L);
        event.setName("Rock Fest");

        Seat seatA = Seat.builder()
                .id(11L)
                .event(event)
                .reserved(false)
                .price(BigDecimal.valueOf(100))
                .build();
        Seat seatB = Seat.builder()
                .id(12L)
                .event(event)
                .reserved(false)
                .price(BigDecimal.valueOf(120))
                .build();

        User user = new User();
        user.setId(42L);

        when(seatRepository.findAllByIdWithEvent(List.of(11L, 12L))).thenReturn(List.of(seatA, seatB));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(ticketRepository.findFirstByUserIdAndEventIdAndStatusOrderByCreatedAtDesc(42L, 5L, Status.RESERVED))
                .thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(1000L);
            }
            return ticket;
        });

        Long ticketId = reservationService.reserveSeats(List.of(11L, 11L, 12L), 42L);

        assertThat(ticketId).isEqualTo(1000L);

        ArgumentCaptor<ReservationEvent> eventCaptor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(reservationEventProducer, times(2)).publish(eventCaptor.capture());

        List<ReservationEvent> publishedEvents = eventCaptor.getAllValues();
        assertThat(publishedEvents)
                .extracting(ReservationEvent::seatId)
                .containsExactlyInAnyOrder(11L, 12L);
        assertThat(publishedEvents)
                .extracting(ReservationEvent::action)
                .containsOnly(ReservationEvent.ReservationAction.RESERVED);
        assertThat(publishedEvents)
                .extracting(ReservationEvent::eventId)
                .containsOnly(5L);
        assertThat(publishedEvents)
                .extracting(ReservationEvent::ticketId)
                .containsOnly(1000L);
    }
}
