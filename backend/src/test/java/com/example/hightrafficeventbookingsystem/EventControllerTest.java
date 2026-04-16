package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.dto.EventResponse;
import com.example.hightrafficeventbookingsystem.dto.SeatResponse;
import com.example.hightrafficeventbookingsystem.model.VenueType;
import com.example.hightrafficeventbookingsystem.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = com.example.hightrafficeventbookingsystem.controller.EventController.class)
class EventControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    EventService eventService;
    @MockitoBean
    com.example.hightrafficeventbookingsystem.security.JwtService jwtService;
    @MockitoBean
    com.example.hightrafficeventbookingsystem.service.UserService userService;

    @Test
    void listEvents_isPublicAndReturns200() throws Exception {
        EventResponse response = new EventResponse(1L, "Koncert", LocalDateTime.now().plusDays(1), VenueType.STADIUM, 4,
                "Ograniczenie biletowe: maksymalnie 4 na osobe", 50L);
        when(eventService.listEvents(any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Koncert"))
                .andExpect(jsonPath("$.content[0].ticketLimitMessage")
                        .value("Ograniczenie biletowe: maksymalnie 4 na osobe"))
                .andExpect(jsonPath("$.content[0].availableSeats").value(50));
    }

    @Test
    void listSeats_isPublicAndReturns200() throws Exception {
        SeatResponse seat = new SeatResponse(10L, 5, 2, "A", "STANDARD", new BigDecimal("50.00"), false);
        when(eventService.listSeats(1L)).thenReturn(List.of(seat));

        mockMvc.perform(get("/api/events/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatNumber").value(5))
                .andExpect(jsonPath("$[0].reserved").value(false));
    }
}
