# Public-Repo


## Entities & Persistence

    - User — email, hashed password, role (ORGANIZER / CUSTOMER)
    - Event — title, venue, date, capacity, available seats, ticket price, linked organizer
    - Booking — customer + event link, ticket count, total amount, status (CONFIRMED / CANCELLED)
    Three JPA Repositories with custom @Query methods for search, filtering by organizer, finding confirmed bookings per event, and summing booked tickets

-- Security (JWT + RBAC)

    - JwtUtils — HS256 token generation and validation via jjwt 0.11.5
    - JwtAuthenticationFilter — OncePerRequestFilter that parses the Authorization: Bearer header on every request
    - SecurityConfig — stateless filter chain that enforces: public (browse events, auth), ROLE_ORGANIZER (/api/organizer/**), ROLE_CUSTOMER (/api/bookings/**)

-- Background Tasks (NotificationService)
    Both tasks use @Async("taskExecutor") on a dedicated ThreadPoolTaskExecutor (4 core / 10 max threads), so they never block the HTTP response:
    TaskTriggerWhat it doesTask 1 — Booking ConfirmationPOST /api/bookings successLogs a formatted email with booking ID, event, tickets, total paidTask 2 — Event Update NotificationPUT /api/organizer/events/{id}Queries all CONFIRMED bookings for the event, logs a notification per customer
- Business Rules enforced in services

    Can't book a past event
    Can't book more tickets than availableSeats
    Can't have two active bookings for the same event (returns 409 Conflict)
    Can't reduce event capacity below already-booked ticket count
    Can't cancel organizer's own events via the customer booking flow
    Cancellation restores seats atomically inside @Transactional

-- Tests — 18 cases

    AuthControllerTest — register, duplicate email (409), validation errors, login, wrong password
    EventBookingIntegrationTest — full happy path, RBAC violations, capacity guard, duplicate booking, cancellation, pagination

