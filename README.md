# banking-ledger-api

[![CI](https://github.com/renangsilveira/banking-ledger-api/actions/workflows/ci.yml/badge.svg)](https://github.com/renangsilveira/banking-ledger-api/actions/workflows/ci.yml)

> Java 21 · Spring Boot 3 · PostgreSQL · Flyway · Testcontainers · Docker

A production-grade double-entry banking ledger API built with Java 21 and Spring Boot 3.

---

## What it does

`banking-ledger-api` is a financial ledger microservice that manages bank accounts and transactions using double-entry accounting principles — every transaction produces exactly two entries (debit + credit), ensuring the ledger is always mathematically balanced.

The service:
- creates and manages bank accounts with real-time balance tracking
- processes deposits, withdrawals, and transfers atomically
- guarantees duplicate-safe operations via `Idempotency-Key` header
- maintains an immutable audit trail of every transaction entry with balance snapshots
- exposes a paginated account statement endpoint with date range filtering
- propagates correlation IDs across all logs for request tracing

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
| Coverage | JaCoCo (80% gate) |
| CI | GitHub Actions |
| Container | Docker + Docker Compose |

---

## Architecture
┌─────────────────────────────────┐
│         REST Clients            │
└────────────────┬────────────────┘
│ HTTP/JSON
┌────────────────▼────────────────┐
│  CorrelationIdFilter            │
├─────────────────────────────────┤
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
│  BalanceService                 │
│  IdempotencyService             │
└────────────────┬────────────────┘
│
┌────────────────▼────────────────┐
│  Spring Data JPA Repositories   │
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

Service starts on `http://localhost:8080`. Flyway runs migrations automatically on startup.

### 3. Full Docker stack

```bash
docker compose --profile app up --build
```

### 4. Run tests

```bash
# All tests
./gradlew test

# With coverage report
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### 5. Open Swagger UI
http://localhost:8080/swagger-ui.html

---

## API reference

### Accounts

| Method | Path | Description | Idempotency |
|--------|------|-------------|-------------|
| `POST` | `/api/v1/accounts` | Create account | Required |
| `GET` | `/api/v1/accounts/{id}` | Get account by ID | — |
| `GET` | `/api/v1/accounts/{id}/balance` | Get current balance | — |
| `GET` | `/api/v1/accounts/{id}/statement` | Get paginated statement | — |

### Transactions

| Method | Path | Description | Idempotency |
|--------|------|-------------|-------------|
| `POST` | `/api/v1/transactions/deposit` | Deposit funds | Required |
| `POST` | `/api/v1/transactions/withdraw` | Withdraw funds | Required |
| `POST` | `/api/v1/transactions/transfer` | Transfer between accounts | Required |
| `GET` | `/api/v1/transactions/{id}` | Get transaction by ID | — |

### Observability

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/actuator/health` | Health check with DB status |
| `GET` | `/actuator/info` | Application info |
| `GET` | `/actuator/metrics` | Available metrics |

### Example requests

**Create account:**
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"holderName": "John Doe", "accountType": "CHECKING", "currency": "BRL"}'
```

**Deposit:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"accountId": "<id>", "amount": 1000.00, "currency": "BRL", "description": "Initial deposit"}'
```

**Transfer:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"sourceAccountId": "<id>", "destinationAccountId": "<id>", "amount": 300.00, "currency": "BRL"}'
```

---

## Key technical decisions

### Double-entry accounting
Every transaction produces exactly two `TransactionEntry` records — one DEBIT and one CREDIT. This ensures the ledger is always mathematically balanced and fully auditable. See [ADR-001](docs/ADR-001-double-entry.md).

### Money as Long (cents)
Monetary values are stored as `Long` (cents) rather than `BigDecimal` or `float`, eliminating floating-point precision issues in financial calculations.

### Pessimistic locking on transfers
Transfers acquire `SELECT FOR UPDATE` locks on both accounts, ordered by account ID to prevent deadlocks under concurrent load. See [ADR-002](docs/ADR-002-pessimistic-locking.md).

### Idempotency
Every mutating operation accepts an `Idempotency-Key` header. Duplicate requests return the original response without re-processing, making the API safe to retry.

### Correlation ID tracing
Every request receives a correlation ID (from `X-Correlation-Id` header or auto-generated) that is propagated through all log entries via MDC.

---

## Documentation

- [System Design Document](docs/SDD.md)
- [ADR-001: Double-entry accounting](docs/ADR-001-double-entry.md)
- [ADR-002: Pessimistic locking](docs/ADR-002-pessimistic-locking.md)

---

## Future improvements

- JWT + Spring Security for authentication
- Circuit Breaker with Resilience4j
- Kafka integration for audit event streaming
- OpenTelemetry distributed tracing
- Optimistic locking migration for read-heavy workloads
- Scheduled job for idempotency key cleanup
