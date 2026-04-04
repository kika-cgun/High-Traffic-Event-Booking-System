package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.dto.ReservationEvent;
import com.example.hightrafficeventbookingsystem.dto.TicketResponse;
import com.example.hightrafficeventbookingsystem.model.*;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import com.example.hightrafficeventbookingsystem.service.ReservationEventProducer;
import com.example.hightrafficeventbookingsystem.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock SeatRepository seatRepository;
    @Mock ReservationEventProducer producer;
    @InjectMocks TicketService ticketService;

    @Test
    void listMyTickets_returnsMappedTickets() {
        Event event = new Event(1L, "Koncert", LocalDateTime.now().plusDays(5), null);
        Seat seat = Seat.builder().id(10L).seatNumber(5).rowNumber(2).section("A").event(event).reserved(true).build();
        Ticket ticket = new Ticket(100L, null, seat, Status.RESERVED, null, LocalDateTime.now());

        when(ticketRepository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(ticket));

        List<TicketResponse> result = ticketService.listMyTickets(42L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(Status.RESERVED);
        assertThat(result.get(0).eventName()).isEqualTo("Koncert");
    }

    @Test
    void confirmReservation_changesStatusToConfirmedAndPublishesKafkaEvent() {
        Event event = new Event(1L, "Koncert", LocalDateTime.now().plusDays(5), null);
        Seat seat = Seat.builder().id(10L).seatNumber(5).rowNumber(2).section("A").event(event).reserved(true).build();
        User user = new User(42L, "jan", "jan@test.com", "pass", null, null, null, null, Role.USER);
        Ticket ticket = new Ticket(100L, user, seat, Status.RESERVED, null, LocalDateTime.now());

        when(ticketRepository.findByIdWithSeatAndEvent(100L)).thenReturn(Optional.of(ticket));

        ticketService.confirmReservation(100L, 42L);

        assertThat(ticket.getStatus()).isEqualTo(Status.CONFIRMED);
        ArgumentCaptor<ReservationEvent> captor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(producer).publish(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo(ReservationEvent.ReservationAction.CONFIRMED);
    }

    @Test
    void confirmReservation_throwsWhenTicketBelongsToDifferentUser() {
        User owner = new User(99L, "other", "other@test.com", "pass", null, null, null, null, Role.USER);
        Seat seat = Seat.builder().id(10L).event(new Event()).reserved(true).build();
        Ticket ticket = new Ticket(100L, owner, seat, Status.RESERVED, null, LocalDateTime.now());

        when(ticketRepository.findByIdWithSeatAndEvent(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.confirmReservation(100L, 42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ticket does not belong to this user");
    }

    @Test
    void confirmReservation_throwsWhenTicketNotReserved() {
        User user = new User(42L, "jan", "jan@test.com", "pass", null, null, null, null, Role.USER);
        Seat seat = Seat.builder().id(10L).event(new Event()).reserved(false).build();
        Ticket ticket = new Ticket(100L, user, seat, Status.CANCELLED, null, LocalDateTime.now());

        when(ticketRepository.findByIdWithSeatAndEvent(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.confirmReservation(100L, 42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only RESERVED tickets can be confirmed");
    }

    @Test
    void cancelReservation_changesStatusAndReleasesSeat() {
        Event event = new Event(1L, "Koncert", LocalDateTime.now().plusDays(5), null);
        Seat seat = Seat.builder().id(10L).seatNumber(5).rowNumber(2).section("A").event(event).reserved(true).build();
        User user = new User(42L, "jan", "jan@test.com", "pass", null, null, null, null, Role.USER);
        Ticket ticket = new Ticket(100L, user, seat, Status.RESERVED, null, LocalDateTime.now());

        when(ticketRepository.findByIdWithSeatAndEvent(100L)).thenReturn(Optional.of(ticket));

        ticketService.cancelReservation(100L, 42L);

        assertThat(ticket.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(seat.isReserved()).isFalse();
        verify(seatRepository).save(seat);
        ArgumentCaptor<ReservationEvent> captor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(producer).publish(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo(ReservationEvent.ReservationAction.CANCELLED);
    }
}
