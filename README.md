# banking-ledger-api

[![CI](https://github.com/renangsilveira/banking-ledger-api/actions/workflows/ci.yml/badge.svg)](https://github.com/renangsilveira/banking-ledger-api/actions/workflows/ci.yml)

> Java 21 · Spring Boot 3 · PostgreSQL · Flyway · Testcontainers · Docker

A production-grade double-entry banking ledger API built with Java 21 and Spring Boot 3.

---

## What it does

`banking-ledger-api` is a financial ledger microservice that manages bank accounts
and transactions using double-entry accounting principles — every transaction produces
exactly two entries (debit + credit), ensuring the ledger is always mathematically balanced.

The service:
- creates and manages bank accounts with real-time balance tracking
- processes deposits, withdrawals, and transfers atomically
- guarantees duplicate-safe operations via `Idempotency-Key`
- maintains an immutable audit trail of every transaction entry
- exposes a paginated account statement endpoint with date range filtering

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 16 + Spring Data JPA + Hibernate |
| Migrations | Flyway |
| API docs | Springdoc OpenAPI / Swagger UI |
| Unit tests | JUnit 5 + Mockito |
| Integration tests | Testcontainers |
| CI | GitHub Actions |
| Container | Docker + Docker Compose |

---

## Architecture
┌─────────────────────────────────┐
│         REST Clients            │
└────────────────┬────────────────┘
│ HTTP
┌────────────────▼────────────────┐
│         Controllers             │
│  AccountController              │
│  TransactionController          │
│  StatementController            │
└────────────────┬────────────────┘
│
┌────────────────▼────────────────┐
│           Services              │
│  AccountService                 │
│  TransactionService             │
│  IdempotencyService             │
└────────────────┬────────────────┘
│
┌────────────────▼────────────────┐
│          Repositories           │
│  Spring Data JPA                │
└────────────────┬────────────────┘
│
┌────────────────▼────────────────┐
│  PostgreSQL 16 + Flyway         │
│  accounts                       │
│  transactions                   │
│  transaction_entries            │
│  idempotency_keys               │
└─────────────────────────────────┘

---

## Running locally

### Prerequisites
- Docker Desktop
- JDK 21

### 1. Start the database

```bash
docker compose up -d
```

### 2. Run the application

```bash
./gradlew bootRun
```

Service starts on `http://localhost:8080`. Flyway runs migrations on startup.

### 3. Full Docker stack

```bash
docker compose --profile app up --build
```

### 4. Run tests

```bash
# All tests (requires Docker for Testcontainers)
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport
# open build/reports/jacoco/test/html/index.html
```

### 5. Open Swagger UI
http://localhost:8080/swagger-ui.html

---

## API reference

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/accounts` | Create account |
| `GET` | `/api/v1/accounts/{id}` | Get account by ID |
| `GET` | `/api/v1/accounts/{id}/balance` | Get current balance |
| `POST` | `/api/v1/transactions/deposit` | Deposit funds |
| `POST` | `/api/v1/transactions/withdraw` | Withdraw funds |
| `POST` | `/api/v1/transactions/transfer` | Transfer between accounts |
| `GET` | `/api/v1/transactions/{id}` | Get transaction details |
| `GET` | `/api/v1/accounts/{id}/statement` | Get paginated statement |
| `GET` | `/actuator/health` | Health check |

---

## Key technical decisions

### Double-entry accounting
Every transaction produces exactly two `TransactionEntry` records — one DEBIT and one CREDIT.
This ensures the ledger is always mathematically balanced and fully auditable.

### Money as Long (cents)
Monetary values are stored as `Long` (cents) rather than `BigDecimal` or `float`,
eliminating floating-point precision issues in financial calculations.

### Pessimistic locking on transfers
Transfers acquire `SELECT FOR UPDATE` locks on both accounts, ordered by account ID
to prevent deadlocks under concurrent load.

### Idempotency
Every mutating operation accepts an `Idempotency-Key` header. Duplicate requests
return the original response without re-processing.

---

## Future improvements

- JWT + Spring Security for authentication
- Circuit Breaker with Resilience4j
- Kafka integration for audit event streaming
- OpenTelemetry distributed tracing
- Optimistic locking migration for read-heavy workloads