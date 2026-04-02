# Design: Frontend + Kafka Completion + WebSocket Real-time

**Date:** 2026-04-02
**Project:** High-Traffic Event Booking System
**Goal:** Portfolio/demo — showcase Spring Boot, Kafka, Redis, WebSocket, Angular Material end-to-end
**Approach:** Incremental (backend gaps → Kafka → WebSocket → frontend)

---

## 1. Scope

Three independent workstreams delivered in order:

1. **Backend REST gaps** — missing GET endpoints + confirm/cancel ticket
2. **Kafka completion** — CONFIRMED/CANCELLED events, AuditLog persist, docker-compose, integration test
3. **WebSocket** — real-time seat status broadcast (Kafka → WS → UI)
4. **Frontend Angular 18** — 6 views, Angular Material dark theme, guest browsing + auth wall at purchase

---

## 2. Backend REST Gaps

### New Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/events` | Public | Paginated list of events |
| `GET` | `/api/events/{id}/seats` | Public | Seat grid for an event |
| `GET` | `/api/tickets/my` | JWT | Authenticated user's tickets |
| `POST` | `/api/tickets/{id}/confirm` | JWT | Confirm reservation (mock pay) |
| `DELETE` | `/api/tickets/{id}` | JWT | Cancel reservation |

### New Classes

- `EventController` — `GET /api/events`, `GET /api/events/{id}/seats`
- `TicketController` — `GET /api/tickets/my`, `POST /api/tickets/{id}/confirm`, `DELETE /api/tickets/{id}`
- `EventService` — list events (paged), list seats by event
- `TicketService` — my tickets, confirm, cancel
- Response DTOs: `EventResponse`, `SeatResponse`, `TicketResponse`

### SecurityConfig Changes

Open publicly (no JWT required):
- `GET /api/events/**`

Everything else remains JWT-protected.

---

## 3. Kafka — Completion

### What's Already Working (do not change)

- `KafkaConfig` — 3-partition topic, idempotent producer, `acks=all`
- `ReservationEventProducer.publish()` — async send with `CompletableFuture`
- `ReservationAuditConsumer` — `@KafkaListener` on `reservation-events`
- `ReservationService.reserveSeat()` — publishes `RESERVED` event

### What's Missing

**1. CONFIRMED and CANCELLED events**

`TicketService.confirmReservation(ticketId, userId)`:
- Validates ownership (IDOR check)
- Changes `Ticket.status` to `CONFIRMED`
- Publishes `ReservationEvent(action=CONFIRMED)`

`TicketService.cancelReservation(ticketId, userId)`:
- Validates ownership
- Changes status to `CANCELLED`, sets `Seat.reserved = false`
- Publishes `ReservationEvent(action=CANCELLED)`

**2. AuditLog persistence**

New entity `AuditLog`:
```
id, ticketId, userId, seatId, eventName, action (enum), partition, offset, timestamp
```

`ReservationAuditConsumer` saves to `AuditLog` on every consumed event (currently only logs).

**3. Docker-compose — Kafka service**

Add Kafka in KRaft mode (no Zookeeper) using Bitnami image:
```yaml
kafka:
  image: bitnami/kafka:3.7
  environment:
    KAFKA_CFG_NODE_ID: 1
    KAFKA_CFG_PROCESS_ROLES: broker,controller
    KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
    KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
  ports:
    - "9092:9092"
```

Update `application.yml`: `KAFKA_BOOTSTRAP_SERVERS=kafka:9092` in docker env.

**4. Integration test**

`ReservationKafkaIntegrationTest` using Testcontainers `KafkaContainer`:
- Call `reserveSeat()` → assert `RESERVED` event lands on topic within 5s
- Consistent with existing `ReservationConcurrencyTest` pattern

---

## 4. WebSocket — Real-time Seat Updates

### Architecture

```
ReservationService / TicketService
    → Kafka (RESERVED / CONFIRMED / CANCELLED event)
        → ReservationAuditConsumer
            → AuditLog (DB persist)
            → SeatWebSocketService.broadcast(eventId, seatId, reserved)
                → /topic/seats/{eventId}
                    → Angular frontend (seat color updates live)
```

### New Classes

**`WebSocketConfig`**
- Enables STOMP over SockJS at `/ws`
- Message broker prefix: `/topic`
- Application destination prefix: `/app`
- Allowed origins: `http://localhost:4200`

**`SeatStatusUpdate` DTO**
```java
record SeatStatusUpdate(Long seatId, Long eventId, boolean reserved) {}
```

**`SeatWebSocketService`**
- Injected into `ReservationAuditConsumer`
- `broadcast(SeatStatusUpdate)` → `SimpMessagingTemplate.convertAndSend("/topic/seats/{eventId}", update)`

### Frontend Integration

On `/events/:id` view:
- Subscribe to `/topic/seats/{eventId}` via `@stomp/rx-stomp`
- On message: find seat by `seatId` in local Signals state, toggle `reserved` flag → color updates instantly
- Works across all open browser tabs simultaneously

---

## 5. Frontend Angular 18

### Tech Stack

- **Angular 18** with standalone components
- **Angular Material** with custom dark theme (deep purple + amber accent)
- **Angular Signals** for state (no NgRx)
- **`@stomp/rx-stomp`** for WebSocket
- **HttpClient** with JWT interceptor

### Project Structure

```
frontend/src/app/
├── core/
│   ├── interceptors/jwt.interceptor.ts
│   ├── guards/auth.guard.ts
│   └── services/
│       ├── auth.service.ts
│       ├── event.service.ts
│       ├── ticket.service.ts
│       └── stomp.service.ts
├── features/
│   ├── auth/
│   │   ├── login/
│   │   └── register/
│   ├── events/
│   │   ├── event-list/
│   │   └── event-detail/     ← seat grid + WebSocket
│   ├── checkout/              ← mock payment
│   └── tickets/              ← my tickets
└── shared/
    ├── components/
    │   ├── seat-grid/
    │   └── ticket-card/
    └── models/               ← TypeScript interfaces
```

### Routing

| Path | Auth Required | Component |
|------|--------------|-----------|
| `/login` | No | `LoginComponent` |
| `/register` | No | `RegisterComponent` |
| `/events` | No (public) | `EventListComponent` |
| `/events/:id` | No (public) | `EventDetailComponent` |
| `/checkout/:ticketId` | Yes | `CheckoutComponent` |
| `/tickets` | Yes | `TicketListComponent` |

`AuthGuard` protects `/checkout` and `/tickets`.

### Auth Wall Behavior

On `/events/:id`, clicking an available seat:
- If logged in → `POST /api/reservations` → redirect to `/checkout/:ticketId`
- If not logged in → redirect to `/login?returnUrl=/events/:id` → after login, returns to same view and continues

### Views

**`/events`** — Material card grid, event name, date, available seat count. Paginator at bottom.

**`/events/:id`** — Seat grid with color coding:
- Green = available (clickable)
- Red = reserved (disabled)
- Colors update live via WebSocket without page refresh

**`/checkout/:ticketId`** — Event name, seat number, price hardcoded as "50 PLN" (Seat/Event entities have no price field — out of scope). "Pay now" button → `MatProgressSpinner` for 2s → `POST /api/tickets/:id/confirm` → success screen with ticket ID.

**`/tickets`** — List of user's tickets with `MatChip` status badge (RESERVED=orange, CONFIRMED=green, CANCELLED=grey). Cancel button on RESERVED tickets.

**`/login` and `/register`** — Centered Material form card, link between the two.

### JWT Interceptor

Reads token from `localStorage`. If present, adds `Authorization: Bearer <token>` to every outgoing request. If 401 received → clear token → redirect to login.

---

## 6. Sequence: Full User Flow

```
[Guest] /events → /events/:id (public, live seat colors)
[Click seat, not logged in] → /login?returnUrl=/events/42
[Login] → back to /events/42 → click seat
    → POST /api/reservations {seatId}
        → Redis lock acquired
        → Seat marked reserved in DB
        → Kafka: RESERVED event
            → AuditLog saved
            → WebSocket: seat turns red for all users
    → redirect to /checkout/:ticketId
[/checkout] → click "Pay Now" → 2s spinner
    → POST /api/tickets/:id/confirm
        → Kafka: CONFIRMED event → AuditLog
    → success screen → link to /tickets
[/tickets] → see CONFIRMED ticket
```

---

## 7. What Is Not In Scope

- Real payment gateway
- Email sending (RabbitMQ NotificationConsumer stays as-is, just logs)
- Admin panel / event management UI
- Venue entity (venueId on Event stays as Long FK placeholder)
- Production deployment / HTTPS / multiple Kafka replicas
