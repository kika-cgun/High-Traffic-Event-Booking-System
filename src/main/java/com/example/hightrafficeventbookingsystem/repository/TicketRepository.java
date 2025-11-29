package com.example.hightrafficeventbookingsystem.repository;

import com.example.hightrafficeventbookingsystem.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
