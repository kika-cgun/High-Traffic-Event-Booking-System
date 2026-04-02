package com.example.hightrafficeventbookingsystem.repository;

import com.example.hightrafficeventbookingsystem.model.Status;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t JOIN FETCH t.seat WHERE t.status = :status AND t.createdAt < :cutoffDateTime")
    List<Ticket> findExpiredWithSeat(Status status, LocalDateTime cutoffDateTime, PageRequest pageable);
}
