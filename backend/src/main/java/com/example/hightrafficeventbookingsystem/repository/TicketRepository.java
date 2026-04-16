package com.example.hightrafficeventbookingsystem.repository;

import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t WHERE t.status = :status AND t.createdAt < :cutoffDateTime")
    List<Ticket> findExpired(Status status, LocalDateTime cutoffDateTime, PageRequest pageable);

    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN FETCH t.seats s LEFT JOIN FETCH s.event WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN FETCH t.seats s LEFT JOIN FETCH s.event WHERE t.id = :id")
    Optional<Ticket> findByIdWithSeatsAndEvent(Long id);

    Optional<Ticket> findFirstByUserIdAndEventIdAndStatusOrderByCreatedAtDesc(Long userId, Long eventId, Status status);
}
