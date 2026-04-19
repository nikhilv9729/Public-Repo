package com.eventbooking.controller;

import com.eventbooking.dto.request.BookingRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.service.impl.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // All endpoints here are ROLE_CUSTOMER only (enforced in SecurityConfig)

    /**
     * POST /api/bookings
     * Book tickets for an event.
     * Triggers Background Task 1 (send confirmation email).
     */
    @PostMapping
    public ResponseEntity<ApiResponse.BookingDetail> bookTickets(
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.bookTickets(request));
    }

    /**
     * GET /api/bookings
     * Get all bookings for the authenticated customer.
     */
    @GetMapping
    public ResponseEntity<ApiResponse.PagedResponse<ApiResponse.BookingDetail>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getMyBookings(page, size));
    }

    /**
     * GET /api/bookings/{id}
     * Get a specific booking by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse.BookingDetail> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    /**
     * DELETE /api/bookings/{id}/cancel
     * Cancel a booking and restore event seats.
     */
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse.BookingDetail> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}
