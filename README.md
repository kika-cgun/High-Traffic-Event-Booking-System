# 🎫 High-Traffic Event Booking System

System rezerwacji biletów na wydarzenia zaprojektowany do obsługi wysokiego ruchu z wykorzystaniem nowoczesnych technologii i wzorców projektowych.

## 📋 Spis treści

- [Opis projektu](#-opis-projektu)
- [Technologie](#-technologie)
- [Architektura](#-architektura)
- [Funkcjonalności](#-funkcjonalności)
- [Wymagania](#-wymagania)
- [Instalacja i uruchomienie](#-instalacja-i-uruchomienie)
- [Konfiguracja](#-konfiguracja)
- [API Dokumentacja](#-api-dokumentacja)
- [Testy](#-testy)
- [Struktura projektu](#-struktura-projektu)

## 🎯 Opis projektu

High-Traffic Event Booking System to zaawansowana aplikacja do zarządzania rezerwacjami biletów na wydarzenia, zbudowana z myślą o obsłudze dużego ruchu i konkurencyjnych żądań. System wykorzystuje mechanizmy rozproszonych blokad (Redis), kolejkowania wiadomości (RabbitMQ) oraz optymistyczne blokowanie bazy danych w celu zapewnienia spójności danych.

### Kluczowe cechy:
- ✅ **Obsługa wysokiego ruchu** - wykorzystanie Redis do distributed locking
- ✅ **Asynchroniczne przetwarzanie** - RabbitMQ do kolejkowania powiadomień
- ✅ **Bezpieczeństwo** - Spring Security z JWT (gotowe do implementacji)
- ✅ **Automatyczne czyszczenie** - zadania scheduled usuwające wygasłe rezerwacje
- ✅ **Dokumentacja API** - Swagger/OpenAPI
- ✅ **Testy integracyjne** - Testcontainers

## 🛠 Technologie

### Backend
- **Java 21** - najnowsza wersja LTS
- **Spring Boot 4.0.0** - framework aplikacji
- **Spring Data JPA** - dostęp do danych
- **Spring Security** - autoryzacja i autentykacja
- **Hibernate** - ORM

### Bazy danych i cache
- **PostgreSQL 15** - relacyjna baza danych
- **Redis** - cache i distributed locking

### Messaging
- **RabbitMQ** - kolejkowanie wiadomości

### Narzędzia i biblioteki
- **Lombok** - redukcja boilerplate code
- **MapStruct** - mapowanie obiektów
- **Springdoc OpenAPI** - dokumentacja API
- **JWT (JJWT)** - tokeny JWT
- **Docker Compose** - konteneryzacja środowiska

### Testy
- **JUnit 5** - framework testowy
- **Testcontainers** - testy integracyjne z kontenerami
- **Spring Security Test** - testy bezpieczeństwa

## 🏗 Architektura

System wykorzystuje klasyczną architekturę warstwową:

```
┌─────────────────────────────────────────┐
│         Controller Layer                │
│  (REST API - ReservationController)     │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Service Layer                   │
│  (Business Logic - ReservationService)  │
│  - Redis Lock Service                   │
│  - Notification Producer/Consumer       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Repository Layer                   │
│  (Data Access - JPA Repositories)       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Database Layer                  │
│  PostgreSQL + Redis + RabbitMQ          │
└─────────────────────────────────────────┘
```

### Mechanizmy ochrony przed współbieżnością:

1. **Distributed Locking (Redis)** - zapobiega jednoczesnym rezerwacjom tego samego miejsca
2. **Optimistic Locking** - wersjonowanie encji w bazie danych
3. **Transakcje** - atomowość operacji bazodanowych
4. **Asynchroniczne powiadomienia** - RabbitMQ odciąża główny wątek aplikacji

## ⚡ Funkcjonalności

### Zarządzanie użytkownikami
- Rejestracja i logowanie użytkowników
- Role: USER, ADMIN
- Autentykacja JWT (gotowe do implementacji)

### Zarządzanie wydarzeniami
- Tworzenie wydarzeń
- Przeglądanie dostępnych wydarzeń
- Zarządzanie miejscami (seats)

### System rezerwacji
- Rezerwacja miejsc z ochroną przed współbieżnością
- Potwierdzanie rezerwacji
- Automatyczne czyszczenie niepotwierdzonych rezerwacji (15 minut)
- Sprawdzanie dostępności miejsc

### Powiadomienia
- Asynchroniczne wysyłanie powiadomień przez RabbitMQ
- Powiadomienia o utworzeniu biletu

### Bezpieczeństwo
- Spring Security z konfiguracją ról
- Przygotowane pod JWT authentication
- Hasła szyfrowane (BCrypt)

## 📦 Wymagania

- **Java 21** lub nowszy
- **Maven 3.8+**
- **Docker** i **Docker Compose**
- **Git**

## 🚀 Instalacja i uruchomienie

### 1. Sklonuj repozytorium

```bash
git clone https://github.com/yourusername/High-Traffic-Event-Booking-System.git
cd High-Traffic-Event-Booking-System
```

### 2. Uruchom usługi Docker (PostgreSQL, Redis, RabbitMQ)

```bash
docker-compose up -d
```

To uruchomi:
- PostgreSQL na porcie `5432`
- Redis na porcie `6379`
- RabbitMQ na porcie `5672` (management console: `15672`)

### 3. Sprawdź czy usługi działają

```bash
docker-compose ps
```

### 4. Zbuduj i uruchom aplikację

```bash
# Zbuduj projekt
mvn clean install

# Uruchom aplikację
mvn spring-boot:run
```

Aplikacja będzie dostępna pod adresem: `http://localhost:8080`

### 5. Dostęp do narzędzi

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **RabbitMQ Management**: http://localhost:15672 (login: `admin`, hasło: `admin`)
- **H2 Console** (jeśli włączone): http://localhost:8080/h2-console

## ⚙️ Konfiguracja

Główna konfiguracja znajduje się w pliku `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/event_booking_system_db
    username: postgres
    password: postgres
  
  data:
    redis:
      host: localhost
      port: 6379
  
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
```

### Konfiguracja środowiskowa

Możesz nadpisać konfigurację używając zmiennych środowiskowych:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/event_booking_system_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_RABBITMQ_HOST=localhost
```

## 📚 API Dokumentacja

Po uruchomieniu aplikacji, pełna dokumentacja API jest dostępna przez Swagger UI:

**URL**: http://localhost:8080/swagger-ui.html

### Przykładowe endpointy:

#### Rezerwacja miejsca
```http
POST /api/reservations/reserve
Content-Type: application/json

{
  "userId": 1,
  "seatId": 5
}
```

#### Potwierdzenie rezerwacji
```http
POST /api/reservations/{ticketId}/confirm
```

#### Sprawdzenie dostępności miejsca
```http
GET /api/reservations/seats/{seatId}/available
```

## 🧪 Testy

Projekt zawiera testy jednostkowe i integracyjne z wykorzystaniem Testcontainers.

### Uruchomienie wszystkich testów

```bash
mvn test
```

### Uruchomienie konkretnego testu

```bash
mvn test -Dtest=ReservationConcurrencyTest
```

### Testy integracyjne

Projekt wykorzystuje **Testcontainers** do testów integracyjnych, co oznacza, że testy automatycznie uruchamiają wymagane kontenery Docker (PostgreSQL, Redis, RabbitMQ).

Przykład: `ReservationConcurrencyTest` - testuje współbieżność rezerwacji.

## 📁 Struktura projektu

```
High-Traffic-Event-Booking-System/
├── src/
│   ├── main/
│   │   ├── java/com/example/hightrafficeventbookingsystem/
│   │   │   ├── config/           # Konfiguracja (Security, RabbitMQ, OpenAPI)
│   │   │   ├── controller/       # REST Controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── init/             # Inicjalizacja danych (DataLoader)
│   │   │   ├── mapper/           # MapStruct mappers
│   │   │   ├── model/            # Encje JPA
│   │   │   │   ├── Event.java
│   │   │   │   ├── User.java
│   │   │   │   ├── Seat.java
│   │   │   │   ├── Ticket.java
│   │   │   │   ├── Role.java
│   │   │   │   └── Status.java
│   │   │   ├── repository/       # JPA Repositories
│   │   │   ├── service/          # Business Logic
│   │   │   │   ├── ReservationService.java
│   │   │   │   ├── RedisLockService.java
│   │   │   │   ├── NotificationProducer.java
│   │   │   │   ├── NotificationConsumer.java
│   │   │   │   └── ReservationCleanupService.java
│   │   │   └── HighTrafficEventBookingSystemApplication.java
│   │   └── resources/
│   │       └── application.yml   # Konfiguracja aplikacji
│   └── test/
│       └── java/
│           └── com/example/hightrafficeventbookingsystem/
│               ├── ReservationConcurrencyTest.java
│               └── TestcontainersConfiguration.java
├── compose.yaml                  # Docker Compose configuration
├── pom.xml                       # Maven dependencies
└── README.md
```

## 🔒 Bezpieczeństwo

### Aktualna konfiguracja

Obecnie aplikacja ma podstawową konfigurację Spring Security z autoryzacją opartą na rolach:
- Rola `USER` - standardowy użytkownik
- Rola `ADMIN` - administrator

### JWT Authentication (gotowe do włączenia)

W pliku `application.yml` znajduje się zakomentowana konfiguracja JWT:

```yaml
# security:
#   jwt:
#     secret-key: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
#     expiration-time: 3600000 # 1 godzina
```

## 🔧 Rozwiązywanie problemów

### Problem: "Unable to determine Dialect without JDBC metadata"

**Rozwiązanie**: Upewnij się, że PostgreSQL działa i jest dostępny:
```bash
docker-compose ps
docker-compose logs postgres
```

### Problem: "FATAL: role 'admin' does not exist"

**Rozwiązanie**: Sprawdź credentials w `application.yml` - domyślnie używany jest user `postgres`:
```yaml
datasource:
  username: postgres
  password: postgres
```

### Problem: Aplikacja nie może połączyć się z Redis/RabbitMQ

**Rozwiązanie**: Sprawdź czy kontenery działają:
```bash
docker-compose up -d
docker-compose logs redis
docker-compose logs rabbitmq
```

## 📈 Kolejne kroki / TODO

- [ ] Implementacja pełnej autentykacji JWT
- [ ] Panel administracyjny
- [ ] System płatności
- [ ] Wysyłanie powiadomień e-mail
- [ ] Statystyki i raporty
- [ ] Rate limiting
- [ ] Caching zapytań

## 👥 Autor

Piotr Capecki - [Twój GitHub](https://github.com/kika-cgun)
