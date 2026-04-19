package com.eventbooking.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventRequest {

    @Data
    public static class Create {
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        private String title;

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        private String description;

        @NotBlank(message = "Venue is required")
        private String venue;

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime eventDate;

        @NotNull(message = "Total capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        private Integer totalCapacity;

        @NotNull(message = "Ticket price is required")
        @DecimalMin(value = "0.00", message = "Ticket price must be non-negative")
        private BigDecimal ticketPrice;
    }

    @Data
    public static class Update {
        @Size(max = 200)
        private String title;

        @Size(max = 2000)
        private String description;

        private String venue;

        @Future(message = "Event date must be in the future")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime eventDate;

        @Min(value = 1)
        private Integer totalCapacity;

        @DecimalMin(value = "0.00")
        private BigDecimal ticketPrice;
    }
}
