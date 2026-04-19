package com.eventbooking.repository;

import com.eventbooking.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByOrganizerIdOrderByEventDateAsc(Long organizerId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventDate > :now ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.availableSeats > 0 AND e.eventDate > :now ORDER BY e.eventDate ASC")
    Page<Event> findAvailableUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE " +
           "(:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND e.eventDate > :now " +
           "ORDER BY e.eventDate ASC")
    Page<Event> searchEvents(@Param("keyword") String keyword,
                             @Param("now") LocalDateTime now,
                             Pageable pageable);

    Optional<Event> findByIdAndOrganizerId(Long id, Long organizerId);
}
