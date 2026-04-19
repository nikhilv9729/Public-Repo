package com.eventbooking.async;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Async background task service.
 *
 * Background Task 1 – Booking Confirmation:
 *   Triggered when a customer successfully books tickets.
 *   Simulates sending a confirmation email via console log.
 *
 * Background Task 2 – Event Update Notification:
 *   Triggered when an organizer updates an event.
 *   Notifies all customers who booked that event via console log.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    // Background Task 1: Booking Confirmation Email
    // ─────────────────────────────────────────────────────────────────────────

    @Async("taskExecutor")
    public void sendBookingConfirmationEmail(Booking booking) {
        try {
            // Simulate email sending latency
            Thread.sleep(500);

            String customerEmail = booking.getCustomer().getEmail();
            String customerName  = booking.getCustomer().getName();
            String eventTitle    = booking.getEvent().getTitle();
            String eventVenue    = booking.getEvent().getVenue();
            String eventDate     = booking.getEvent().getEventDate().format(FORMATTER);

            log.info("""
                    ╔══════════════════════════════════════════════════════════╗
                    ║         [BACKGROUND TASK 1] BOOKING CONFIRMATION EMAIL   ║
                    ╠══════════════════════════════════════════════════════════╣
                    ║  TO      : {}
                    ║  SUBJECT : Booking Confirmed – {}
                    ║  ──────────────────────────────────────────────────────  ║
                    ║  Dear {},
                    ║                                                          ║
                    ║  Your booking has been confirmed! 🎉                     ║
                    ║                                                          ║
                    ║  Booking ID   : #{}
                    ║  Event        : {}
                    ║  Venue        : {}
                    ║  Date         : {}
                    ║  Tickets      : {}
                    ║  Total Paid   : ${}
                    ║                                                          ║
                    ║  Thank you for booking with EventHub!                    ║
                    ╚══════════════════════════════════════════════════════════╝
                    """,
                    customerEmail,
                    eventTitle,
                    customerName,
                    booking.getId(),
                    eventTitle,
                    eventVenue,
                    eventDate,
                    booking.getNumberOfTickets(),
                    booking.getTotalAmount()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[BACKGROUND TASK 1] Interrupted while sending booking confirmation for booking #{}",
                    booking.getId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Background Task 2: Event Update Notification
    // ─────────────────────────────────────────────────────────────────────────

    @Async("taskExecutor")
    public void sendEventUpdateNotifications(Event event, List<Booking> affectedBookings) {
        if (affectedBookings == null || affectedBookings.isEmpty()) {
            log.info("[BACKGROUND TASK 2] Event '{}' updated — no confirmed bookings to notify.", event.getTitle());
            return;
        }

        log.info("[BACKGROUND TASK 2] Event update detected for '{}'. Notifying {} customer(s)...",
                event.getTitle(), affectedBookings.size());

        for (Booking booking : affectedBookings) {
            try {
                Thread.sleep(200); // simulate per-email delay

                String customerEmail = booking.getCustomer().getEmail();
                String customerName  = booking.getCustomer().getName();
                String eventDate     = event.getEventDate().format(FORMATTER);

                log.info("""
                        ╔══════════════════════════════════════════════════════════╗
                        ║      [BACKGROUND TASK 2] EVENT UPDATE NOTIFICATION       ║
                        ╠══════════════════════════════════════════════════════════╣
                        ║  TO      : {}
                        ║  SUBJECT : Important update for your booking – {}
                        ║  ──────────────────────────────────────────────────────  ║
                        ║  Dear {},
                        ║                                                          ║
                        ║  The event you booked has been updated by the organizer. ║
                        ║                                                          ║
                        ║  Event      : {}
                        ║  New Venue  : {}
                        ║  New Date   : {}
                        ║  New Price  : ${}
                        ║  Booking ID : #{}
                        ║                                                          ║
                        ║  Please review the changes. Contact the organizer        ║
                        ║  if you have any questions.                              ║
                        ╚══════════════════════════════════════════════════════════╝
                        """,
                        customerEmail,
                        event.getTitle(),
                        customerName,
                        event.getTitle(),
                        event.getVenue(),
                        eventDate,
                        event.getTicketPrice(),
                        booking.getId()
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[BACKGROUND TASK 2] Interrupted while notifying customer {} for event update.",
                        booking.getCustomer().getEmail());
            }
        }

        log.info("[BACKGROUND TASK 2] Finished notifying all {} customer(s) for event '{}'.",
                affectedBookings.size(), event.getTitle());
    }
}
