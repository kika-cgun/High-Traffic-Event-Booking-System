package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.dto.EventResponse;
import com.example.hightrafficeventbookingsystem.dto.SeatResponse;
import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.VenueType;
import com.example.hightrafficeventbookingsystem.repository.EventRepository;
import com.example.hightrafficeventbookingsystem.repository.SeatRepository;
import com.example.hightrafficeventbookingsystem.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock SeatRepository seatRepository;
    @InjectMocks EventService eventService;

    @Test
    void listEvents_returnsMappedPage() {
        Event event = new Event(1L, "Koncert", LocalDateTime.now().plusDays(5), VenueType.CINEMA, 8, null);
        when(eventRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(seatRepository.findByEventId(1L)).thenReturn(List.of());

        Page<EventResponse> result = eventService.listEvents(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Koncert");
        assertThat(result.getContent().get(0).availableSeats()).isZero();
    }

    @Test
    void listSeats_returnsMappedSeats() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        Seat seat = Seat.builder()
                .id(10L).seatNumber(5).rowNumber(2).section("A").reserved(false)
                .build();
        when(seatRepository.findByEventId(1L)).thenReturn(List.of(seat));

        List<SeatResponse> result = eventService.listSeats(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).seatNumber()).isEqualTo(5);
        assertThat(result.get(0).reserved()).isFalse();
    }

    @Test
    void listSeats_throwsWhenEventNotFound() {
        when(eventRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> eventService.listSeats(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event not found");
    }
}
