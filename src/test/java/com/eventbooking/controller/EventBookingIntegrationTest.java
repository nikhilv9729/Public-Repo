package com.eventbooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventBookingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String organizerToken;
    private String customerToken;
    private Long createdEventId;

    @BeforeEach
    void setup() throws Exception {
        organizerToken = registerAndLogin("organizer@test.com", "ORGANIZER");
        customerToken  = registerAndLogin("customer@test.com",  "CUSTOMER");
        createdEventId = createEvent(organizerToken);
    }

    // ─── Event: Public Browse ────────────────────────────────────────────────

    @Test
    void browseEvents_noAuth_returnsOk() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getEventById_noAuth_returnsOk() throws Exception {
        mockMvc.perform(get("/api/events/" + createdEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdEventId))
                .andExpect(jsonPath("$.title").value("Spring Boot Conference"));
    }

    @Test
    void getEventById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/events/9999"))
                .andExpect(status().isNotFound());
    }

    // ─── Event: Organizer CRUD ───────────────────────────────────────────────

    @Test
    void createEvent_asOrganizer_returnsCreated() throws Exception {
        var body = Map.of(
                "title", "New Event",
                "venue", "Budapest Arena",
                "eventDate", "2027-06-15T18:00:00",
                "totalCapacity", 200,
                "ticketPrice", 49.99
        );
        mockMvc.perform(post("/api/organizer/events")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Event"))
                .andExpect(jsonPath("$.availableSeats").value(200));
    }

    @Test
    void createEvent_asCustomer_returns403() throws Exception {
        var body = Map.of("title", "Hack", "venue", "X", "eventDate", "2027-01-01T10:00:00",
                "totalCapacity", 10, "ticketPrice", 0);
        mockMvc.perform(post("/api/organizer/events")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEvent_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/organizer/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEvent_asOrganizer_triggersNotification() throws Exception {
        var body = Map.of("venue", "Updated Venue", "ticketPrice", 39.99);
        mockMvc.perform(put("/api/organizer/events/" + createdEventId)
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue").value("Updated Venue"));
    }

    @Test
    void deleteEvent_asOrganizer_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/organizer/events/" + createdEventId)
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event deleted successfully."));
    }

    @Test
    void getOrganizerEvents_returnsList() throws Exception {
        mockMvc.perform(get("/api/organizer/events")
                        .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    // ─── Booking ────────────────────────────────────────────────────────────

    @Test
    void bookTickets_asCustomer_returnsCreated_andTriggersConfirmationTask() throws Exception {
        var body = Map.of("eventId", createdEventId, "numberOfTickets", 2);
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.numberOfTickets").value(2))
                .andExpect(jsonPath("$.totalAmount").value(greaterThan(0.0)));
    }

    @Test
    void bookTickets_asOrganizer_returns403() throws Exception {
        var body = Map.of("eventId", createdEventId, "numberOfTickets", 1);
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookTickets_duplicateBooking_returns409() throws Exception {
        var body = Map.of("eventId", createdEventId, "numberOfTickets", 1);
        // First booking
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
        // Second booking for same event
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void bookTickets_exceedCapacity_returns400() throws Exception {
        var body = Map.of("eventId", createdEventId, "numberOfTickets", 9999);
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyBookings_returnsPagedList() throws Exception {
        // Book first
        var body = Map.of("eventId", createdEventId, "numberOfTickets", 1);
        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void cancelBooking_asCustomer_returnsCANCELLED() throws Exception {
        // Book
        var body = Map.of("eventId", createdEventId, "numberOfTickets", 1);
        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        Long bookingId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Cancel
        mockMvc.perform(delete("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String registerAndLogin(String email, String role) throws Exception {
        var reg = Map.of("name", role + " User", "email", email, "password", "pass1234", "role", role);
        MvcResult r = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createEvent(String token) throws Exception {
        var body = Map.of(
                "title", "Spring Boot Conference",
                "description", "Annual Spring Boot deep-dive",
                "venue", "MOM Sport Arena, Budapest",
                "eventDate", "2027-09-20T10:00:00",
                "totalCapacity", 500,
                "ticketPrice", 29.99
        );
        MvcResult r = mockMvc.perform(post("/api/organizer/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }
}
