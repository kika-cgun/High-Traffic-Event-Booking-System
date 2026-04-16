package com.example.hightrafficeventbookingsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Event selected during checkout. Needed before seats are truly reserved.
     */
    private Long eventId;

    /**
     * Comma-separated seat IDs selected during checkout and finalized on Pay Now.
     */
    @Column(length = 1000)
    private String requestedSeatIds;

    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String paymentId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
