package com.example.hightrafficeventbookingsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "section", "row_number", "seat_number"})
})
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer seatNumber;
    private Integer rowNumber;
    private String section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private Boolean reserved = false;

    @Version
    private Integer version;

    private BigDecimal price;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    public boolean isReserved() {
        return Boolean.TRUE.equals(this.reserved);
    }
}
