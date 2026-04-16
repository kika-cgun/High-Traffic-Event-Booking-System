package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.config.SecurityConfig;
import com.example.hightrafficeventbookingsystem.dto.SeatInfo;
import com.example.hightrafficeventbookingsystem.dto.TicketResponse;
import com.example.hightrafficeventbookingsystem.model.Role;
import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.model.VenueType;
import com.example.hightrafficeventbookingsystem.security.JwtAuthenticationFilter;
import com.example.hightrafficeventbookingsystem.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = com.example.hightrafficeventbookingsystem.controller.TicketController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
class TicketControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean TicketService ticketService;

    private User janUser() {
        User u = new User();
        u.setId(42L);
        u.setUsername("jan");
        u.setRole(Role.USER);
        return u;
    }

    @Test
    void listMyTickets_returns200() throws Exception {
        TicketResponse ticket = new TicketResponse(
                1L, Status.RESERVED, LocalDateTime.now(), "Koncert",
                LocalDateTime.now().plusDays(10), VenueType.STADIUM,
                List.of(new SeatInfo(10L, 5, 2, "A", "STANDARD", new BigDecimal("150.00"))),
                new BigDecimal("150.00")
        );
        when(ticketService.listMyTickets(42L)).thenReturn(List.of(ticket));

        mockMvc.perform(get("/api/tickets/my").with(user(janUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESERVED"))
                .andExpect(jsonPath("$[0].eventName").value("Koncert"));
    }

    @Test
    void confirmReservation_returns204() throws Exception {
        mockMvc.perform(post("/api/tickets/1/confirm").with(user(janUser())))
                .andExpect(status().isNoContent());
        verify(ticketService).confirmReservation(1L, 42L);
    }

    @Test
    void cancelReservation_returns204() throws Exception {
        mockMvc.perform(delete("/api/tickets/1").with(user(janUser())))
                .andExpect(status().isNoContent());
        verify(ticketService).cancelReservation(1L, 42L);
    }
}
