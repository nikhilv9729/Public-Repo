package com.eventbooking.controller;

import com.eventbooking.dto.request.EventRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.service.impl.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ═══════════════════════════════════════════════════════════════
    //  PUBLIC ENDPOINTS — accessible to everyone (no auth required)
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /api/events?keyword=&page=0&size=10
     * Browse upcoming events with optional keyword search.
     */
    @GetMapping("/api/events")
    public ResponseEntity<ApiResponse.PagedResponse<ApiResponse.EventDetail>> browseEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.browseEvents(keyword, page, size));
    }

    /**
     * GET /api/events/{id}
     * Get event details by ID.
     */
    @GetMapping("/api/events/{id}")
    public ResponseEntity<ApiResponse.EventDetail> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    // ═══════════════════════════════════════════════════════════════
    //  ORGANIZER ENDPOINTS — ROLE_ORGANIZER only
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/organizer/events
     * Create a new event.
     */
    @PostMapping("/api/organizer/events")
    public ResponseEntity<ApiResponse.EventDetail> createEvent(
            @Valid @RequestBody EventRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    /**
     * PUT /api/organizer/events/{id}
     * Update an event. Triggers Background Task 2 (notify booked customers).
     */
    @PutMapping("/api/organizer/events/{id}")
    public ResponseEntity<ApiResponse.EventDetail> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest.Update request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    /**
     * DELETE /api/organizer/events/{id}
     * Delete an event.
     */
    @DeleteMapping("/api/organizer/events/{id}")
    public ResponseEntity<ApiResponse.MessageResponse> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(new ApiResponse.MessageResponse("Event deleted successfully."));
    }

    /**
     * GET /api/organizer/events?page=0&size=10
     * List all events created by the authenticated organizer.
     */
    @GetMapping("/api/organizer/events")
    public ResponseEntity<ApiResponse.PagedResponse<ApiResponse.EventDetail>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getMyEvents(page, size));
    }
}
