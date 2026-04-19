package com.eventbooking.dto.response;

import com.eventbooking.enums.BookingStatus;
import com.eventbooking.enums.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    // ──────────────── Auth ────────────────
    @Data @Builder
    public static class AuthToken {
        private String token;
        private String type;
        private String email;
        private String name;
        private Role role;
    }

    // ──────────────── User ────────────────
    @Data @Builder
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private Role role;
    }

    // ──────────────── Event ────────────────
    @Data @Builder
    public static class EventDetail {
        private Long id;
        private String title;
        private String description;
        private String venue;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime eventDate;

        private Integer totalCapacity;
        private Integer availableSeats;
        private BigDecimal ticketPrice;
        private UserInfo organizer;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;
    }

    // ──────────────── Booking ────────────────
    @Data @Builder
    public static class BookingDetail {
        private Long id;
        private Long eventId;
        private String eventTitle;
        private String eventVenue;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime eventDate;

        private Integer numberOfTickets;
        private BigDecimal totalAmount;
        private BookingStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime bookedAt;
    }

    // ──────────────── Generic ────────────────
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class MessageResponse {
        private String message;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PagedResponse<T> {
        private java.util.List<T> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
