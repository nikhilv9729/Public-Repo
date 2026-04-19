package com.eventbooking.service.impl;

import com.eventbooking.async.NotificationService;
import com.eventbooking.dto.request.EventRequest;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    // ─── Organizer Operations ────────────────────────────────────────────────

    @Transactional
    public ApiResponse.EventDetail createEvent(EventRequest.Create request) {
        User organizer = getCurrentUser();

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .totalCapacity(request.getTotalCapacity())
                .availableSeats(request.getTotalCapacity())
                .ticketPrice(request.getTicketPrice())
                .organizer(organizer)
                .build();

        event = eventRepository.save(event);
        return toDetailDto(event);
    }

    @Transactional
    public ApiResponse.EventDetail updateEvent(Long eventId, EventRequest.Update request) {
        User organizer = getCurrentUser();

        Event event = eventRepository.findByIdAndOrganizerId(eventId, organizer.getId())
                .orElseThrow(() -> new AppException.ResourceNotFoundException(
                        "Event not found or you are not the organizer."));

        boolean hasChanges = false;

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
            hasChanges = true;
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
            hasChanges = true;
        }
        if (request.getVenue() != null) {
            event.setVenue(request.getVenue());
            hasChanges = true;
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
            hasChanges = true;
        }
        if (request.getTicketPrice() != null) {
            event.setTicketPrice(request.getTicketPrice());
            hasChanges = true;
        }
        if (request.getTotalCapacity() != null) {
            int bookedTickets = bookingRepository.sumTicketsByEventId(eventId) != null
                    ? bookingRepository.sumTicketsByEventId(eventId) : 0;
            if (request.getTotalCapacity() < bookedTickets) {
                throw new AppException.BadRequestException(
                        "New capacity (" + request.getTotalCapacity() + ") is less than already booked tickets ("
                        + bookedTickets + ").");
            }
            event.setTotalCapacity(request.getTotalCapacity());
            event.setAvailableSeats(request.getTotalCapacity() - bookedTickets);
            hasChanges = true;
        }

        event = eventRepository.save(event);

        // ── Background Task 2: Notify all confirmed customers about the update ──
        if (hasChanges) {
            List<Booking> confirmedBookings = bookingRepository.findByEventIdAndStatus(eventId, BookingStatus.CONFIRMED);
            notificationService.sendEventUpdateNotifications(event, confirmedBookings);
        }

        return toDetailDto(event);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        User organizer = getCurrentUser();
        Event event = eventRepository.findByIdAndOrganizerId(eventId, organizer.getId())
                .orElseThrow(() -> new AppException.ResourceNotFoundException(
                        "Event not found or you are not the organizer."));
        eventRepository.delete(event);
    }

    public ApiResponse.PagedResponse<ApiResponse.EventDetail> getMyEvents(int page, int size) {
        User organizer = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> events = eventRepository.findByOrganizerIdOrderByEventDateAsc(organizer.getId(), pageable);
        return toPagedResponse(events);
    }

    // ─── Public / Customer Operations ───────────────────────────────────────

    public ApiResponse.PagedResponse<ApiResponse.EventDetail> browseEvents(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> events;

        if (keyword != null && !keyword.isBlank()) {
            events = eventRepository.searchEvents(keyword.trim(), LocalDateTime.now(), pageable);
        } else {
            events = eventRepository.findAvailableUpcomingEvents(LocalDateTime.now(), pageable);
        }
        return toPagedResponse(events);
    }

    public ApiResponse.EventDetail getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException.ResourceNotFoundException("Event not found with id: " + eventId));
        return toDetailDto(event);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.ResourceNotFoundException("Authenticated user not found."));
    }

    private ApiResponse.EventDetail toDetailDto(Event event) {
        ApiResponse.UserInfo organizerInfo = ApiResponse.UserInfo.builder()
                .id(event.getOrganizer().getId())
                .name(event.getOrganizer().getName())
                .email(event.getOrganizer().getEmail())
                .role(event.getOrganizer().getRole())
                .build();

        return ApiResponse.EventDetail.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .totalCapacity(event.getTotalCapacity())
                .availableSeats(event.getAvailableSeats())
                .ticketPrice(event.getTicketPrice())
                .organizer(organizerInfo)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private ApiResponse.PagedResponse<ApiResponse.EventDetail> toPagedResponse(Page<Event> page) {
        List<ApiResponse.EventDetail> content = page.getContent().stream()
                .map(this::toDetailDto)
                .toList();

        return ApiResponse.PagedResponse.<ApiResponse.EventDetail>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
