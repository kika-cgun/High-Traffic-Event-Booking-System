package com.example.hightrafficeventbookingsystem.repository;

import com.example.hightrafficeventbookingsystem.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT s FROM Seat s JOIN FETCH s.event WHERE s.id = :id")
    Optional<Seat> findByIdWithEvent(Long id);

    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId ORDER BY s.rowNumber ASC, s.seatNumber ASC")
    List<Seat> findByEventId(Long eventId);

    @Query("SELECT s FROM Seat s JOIN FETCH s.event WHERE s.id IN :ids")
    List<Seat> findAllByIdWithEvent(Collection<Long> ids);
}
