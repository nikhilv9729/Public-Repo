package com.eventbooking.service.impl;

import com.eventbooking.async.NotificationService;
import com.eventbooking.dto.request.BookingRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.User;
import com.eventbooking.enums.BookingStatus;
import com.eventbooking.exception.AppException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ApiResponse.BookingDetail bookTickets(BookingRequest request) {
        User customer = getCurrentUser();
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new AppException.ResourceNotFoundException(
                        "Event not found with id: " + request.getEventId()));

        // Guard: event must be in the future
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new AppException.BadRequestException("Cannot book tickets for a past event.");
        }

        // Guard: seats available
        if (event.getAvailableSeats() < request.getNumberOfTickets()) {
            throw new AppException.BadRequestException(
                    "Not enough seats. Requested: " + request.getNumberOfTickets()
                    + ", Available: " + event.getAvailableSeats());
        }

        // Guard: no duplicate active booking
        if (bookingRepository.existsByCustomerIdAndEventIdAndStatus(
                customer.getId(), event.getId(), BookingStatus.CONFIRMED)) {
            throw new AppException.ConflictException(
                    "You already have a confirmed booking for this event. " +
                    "Cancel it first if you want to re-book.");
        }

        // Deduct seats (optimistic — protected by @Transactional)
        event.setAvailableSeats(event.getAvailableSeats() - request.getNumberOfTickets());
        eventRepository.save(event);

        BigDecimal totalAmount = event.getTicketPrice()
                .multiply(BigDecimal.valueOf(request.getNumberOfTickets()));

        Booking booking = Booking.builder()
                .customer(customer)
                .event(event)
                .numberOfTickets(request.getNumberOfTickets())
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .build();

        booking = bookingRepository.save(booking);

        // ── Background Task 1: Send booking confirmation email ──
        notificationService.sendBookingConfirmationEmail(booking);

        return toDto(booking);
    }

    @Transactional
    public ApiResponse.BookingDetail cancelBooking(Long bookingId) {
        User customer = getCurrentUser();

        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new AppException.ResourceNotFoundException(
                        "Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException.BadRequestException("Booking is already cancelled.");
        }

        if (booking.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new AppException.BadRequestException("Cannot cancel a booking for a past event.");
        }

        // Restore seats
        Event event = booking.getEvent();
        event.setAvailableSeats(event.getAvailableSeats() + booking.getNumberOfTickets());
        eventRepository.save(event);

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        return toDto(booking);
    }

    public ApiResponse.PagedResponse<ApiResponse.BookingDetail> getMyBookings(int page, int size) {
        User customer = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = bookingRepository.findByCustomerIdOrderByBookedAtDesc(customer.getId(), pageable);

        List<ApiResponse.BookingDetail> content = bookings.getContent().stream()
                .map(this::toDto)
                .toList();

        return ApiResponse.PagedResponse.<ApiResponse.BookingDetail>builder()
                .content(content)
                .pageNumber(bookings.getNumber())
                .pageSize(bookings.getSize())
                .totalElements(bookings.getTotalElements())
                .totalPages(bookings.getTotalPages())
                .last(bookings.isLast())
                .build();
    }

    public ApiResponse.BookingDetail getBookingById(Long bookingId) {
        User customer = getCurrentUser();
        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customer.getId())
                .orElseThrow(() -> new AppException.ResourceNotFoundException(
                        "Booking not found with id: " + bookingId));
        return toDto(booking);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.ResourceNotFoundException("Authenticated user not found."));
    }

    private ApiResponse.BookingDetail toDto(Booking booking) {
        return ApiResponse.BookingDetail.builder()
                .id(booking.getId())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .eventVenue(booking.getEvent().getVenue())
                .eventDate(booking.getEvent().getEventDate())
                .numberOfTickets(booking.getNumberOfTickets())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .bookedAt(booking.getBookedAt())
                .build();
    }
}
