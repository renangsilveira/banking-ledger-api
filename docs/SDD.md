# System Design Document — banking-ledger-api

## 1. Overview

`banking-ledger-api` is a financial ledger microservice that manages bank accounts and transactions using **double-entry accounting** principles. Every transaction produces exactly two `TransactionEntry` records (DEBIT + CREDIT), ensuring the ledger is always mathematically balanced and fully auditable.

---

## 2. Goals

- Provide a reliable API for creating accounts and processing financial transactions
- Guarantee ledger consistency through double-entry accounting
- Prevent duplicate processing via idempotency keys
- Ensure data integrity under concurrent load via pessimistic locking
- Expose a paginated, filterable account statement endpoint

---

## 3. Non-Goals

- Authentication and authorization (planned for future iterations)
- Multi-currency conversion
- Real-time event streaming (planned: Kafka integration)
- Distributed tracing (planned: OpenTelemetry)

---

## 4. Architecture
┌─────────────────────────────────┐
│         REST Clients            │
└────────────────┬────────────────┘
│ HTTP/JSON
┌────────────────▼────────────────┐
│  CorrelationIdFilter            │  → injects X-Correlation-Id into MDC
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
│  PostgreSQL 16                  │
│  (Flyway-managed schema)        │
└─────────────────────────────────┘

---

## 5. Domain Model

### Account
Represents a bank account. Holds balance as `Long` (cents) to avoid floating-point precision issues. Business rules (`debit`, `credit`) are encapsulated in the entity (rich domain model).

### Money
An `@Embeddable` value object that stores amount in cents and currency as ISO 4217 code. All arithmetic operations validate currency compatibility.

### Transaction
Represents a financial event (DEPOSIT, WITHDRAWAL, TRANSFER). Always paired with exactly two `TransactionEntry` records.

### TransactionEntry
Immutable record of a single side of a transaction (DEBIT or CREDIT). Stores `balanceBefore` and `balanceAfter` snapshots for full audit trail.

### IdempotencyKey
Stores processed request keys with a 24-hour TTL, preventing duplicate processing of the same operation.

---

## 6. Database Schema

```sql
accounts (
  id               UUID PK,
  account_number   VARCHAR(20) UNIQUE,
  holder_name      VARCHAR(255),
  account_type     VARCHAR(20),   -- CHECKING | SAVINGS
  status           VARCHAR(20),   -- ACTIVE | INACTIVE | BLOCKED
  balance_amount   BIGINT,        -- stored in cents
  balance_currency VARCHAR(3),    -- ISO 4217
  idempotency_key  VARCHAR(255) UNIQUE,
  created_at       TIMESTAMP,
  updated_at       TIMESTAMP
)

transactions (
  id               UUID PK,
  idempotency_key  VARCHAR(255) UNIQUE,
  type             VARCHAR(20),   -- DEPOSIT | WITHDRAWAL | TRANSFER
  status           VARCHAR(20),   -- COMPLETED | FAILED
  amount           BIGINT,
  currency         VARCHAR(3),
  description      VARCHAR(500),
  correlation_id   VARCHAR(255),
  created_at       TIMESTAMP
)

transaction_entries (
  id             UUID PK,
  transaction_id UUID FK → transactions,
  account_id     UUID FK → accounts,
  entry_type     VARCHAR(10),  -- DEBIT | CREDIT
  amount         BIGINT,
  currency       VARCHAR(3),
  balance_before BIGINT,       -- snapshot before operation
  balance_after  BIGINT,       -- snapshot after operation
  created_at     TIMESTAMP
)

idempotency_keys (
  id            UUID PK,
  key           VARCHAR(255) UNIQUE,
  entity_type   VARCHAR(50),
  entity_id     UUID,
  http_status   INT,
  response_body TEXT,
  created_at    TIMESTAMP,
  expires_at    TIMESTAMP
)
```

---

## 7. API Contracts

| Method | Path | Description | Idempotency |
|--------|------|-------------|-------------|
| `POST` | `/api/v1/accounts` | Create account | Required |
| `GET` | `/api/v1/accounts/{id}` | Get account | — |
| `GET` | `/api/v1/accounts/{id}/balance` | Get balance | — |
| `POST` | `/api/v1/transactions/deposit` | Deposit funds | Required |
| `POST` | `/api/v1/transactions/withdraw` | Withdraw funds | Required |
| `POST` | `/api/v1/transactions/transfer` | Transfer between accounts | Required |
| `GET` | `/api/v1/transactions/{id}` | Get transaction | — |
| `GET` | `/api/v1/accounts/{id}/statement` | Paginated statement | — |
| `GET` | `/actuator/health` | Health check | — |

All mutating operations require an `Idempotency-Key` header. Duplicate requests with the same key return the original response without re-processing.

---

## 8. Double-Entry Accounting

Every transaction creates exactly **two** `TransactionEntry` records:
DEPOSIT of R$ 1000.00 into account A:
Entry 1: account_a | CREDIT | 1000.00 | balance_before=0     | balance_after=1000
Entry 2: account_a | DEBIT  | 1000.00 | balance_before=0     | balance_after=0
TRANSFER of R$ 300.00 from account A to account B:
Entry 1: account_a | DEBIT  | 300.00  | balance_before=1000  | balance_after=700
Entry 2: account_b | CREDIT | 300.00  | balance_before=0     | balance_after=300

This guarantees: `SUM(all CREDIT entries) - SUM(all DEBIT entries) = current balance`

---

## 9. Idempotency Strategy

Three-layer protection:

1. **Pre-check**: query `idempotency_keys` table before processing
2. **DB unique constraint**: `idempotency_key` column is `UNIQUE` — concurrent duplicates fail at DB level
3. **Duplicate resolution**: on duplicate, fetch original entity from source table and return

---

## 10. Locking Strategy

Transfers use **pessimistic locking** (`SELECT FOR UPDATE`) to prevent race conditions:

1. Both accounts are locked before any balance modification
2. Lock acquisition order is **always sorted by account UUID** to prevent deadlocks
3. Locks are released automatically at transaction commit/rollback

Concurrency test (`TransferConcurrencyIT`) validates 10 concurrent transfers never produce a negative balance.

---

## 11. Test Matrix

| Test class | Type | Coverage |
|---|---|---|
| `MoneyTest` | Unit | Value object arithmetic, edge cases |
| `TransactionServiceTest` | Unit | Business rules, exceptions |
| `AccountControllerIT` | Integration | HTTP endpoints, validation, idempotency |
| `TransactionControllerIT` | Integration | Deposit, withdrawal, transfer flows |
| `StatementControllerIT` | Integration | Pagination, ordering, date filtering |
| `TransferConcurrencyIT` | Concurrency | 10 threads, no overdraft, ledger balance |

---

## 12. Future Improvements

- **Authentication**: JWT + Spring Security
- **Resilience**: Circuit Breaker with Resilience4j
- **Observability**: OpenTelemetry distributed tracing
- **Event streaming**: Kafka integration for audit events
- **Locking migration**: Optimistic locking for read-heavy workloads
- **Idempotency cleanup**: Scheduled job to purge expired keys
