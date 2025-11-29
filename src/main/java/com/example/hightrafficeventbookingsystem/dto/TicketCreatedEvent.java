package com.example.hightrafficeventbookingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketCreatedEvent{
    private Long ticketId;
    private String userEmail;
}
