package com.eventbooking.repository;

import com.eventbooking.entity.Booking;
import com.eventbooking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByCustomerIdOrderByBookedAtDesc(Long customerId, Pageable pageable);

    Optional<Booking> findByIdAndCustomerId(Long bookingId, Long customerId);

    boolean existsByCustomerIdAndEventIdAndStatus(Long customerId, Long eventId, BookingStatus status);

    @Query("SELECT b FROM Booking b JOIN FETCH b.customer WHERE b.event.id = :eventId AND b.status = :status")
    List<Booking> findByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") BookingStatus status);

    @Query("SELECT SUM(b.numberOfTickets) FROM Booking b WHERE b.event.id = :eventId AND b.status = 'CONFIRMED'")
    Integer sumTicketsByEventId(@Param("eventId") Long eventId);
}
