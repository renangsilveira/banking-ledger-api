# Portfolio Positioning — banking-ledger-api

## One-paragraph pitch

`banking-ledger-api` is a production-grade double-entry ledger service built to demonstrate the core financial-systems pattern every fintech backend eventually needs: a mathematically balanced, fully auditable transaction model. Every operation — deposit, withdrawal, transfer — produces exactly two entries (debit + credit), is idempotent via a client-supplied `Idempotency-Key`, and leaves an immutable audit trail with balance snapshots. Money is modeled as integer cents rather than floating-point, and concurrent transfers are protected with ordered pessimistic locking to prevent deadlocks. The project pairs a clean layered architecture (controllers → services → JPA repositories → PostgreSQL/Flyway) with the documentation discipline — ADRs, an SDD, Testcontainers integration tests, and an 80% JaCoCo gate — expected of a senior engineer working in regulated financial infrastructure.

## Interview talking points

- **Why double-entry accounting, not just a `balance` column?**
  A single mutable balance field can drift from reality under concurrent writes or partial failures. Double-entry makes every change reconstructable from its entries — the balance is a derived value, not a source of truth — which is how real ledgers (and auditors) reason about correctness.

- **Why cents as `Long` instead of `BigDecimal`?**
  Floating-point and even naive `BigDecimal` usage can introduce rounding inconsistencies across currencies and operations at scale. Integer minor-units arithmetic is simpler to reason about, faster, and matches how payment processors like Stripe represent money internally.

- **How do you prevent double-processing on retries?**
  Every mutating endpoint requires an `Idempotency-Key` header. A duplicate key short-circuits to the original stored response instead of re-executing the transaction — the same pattern Stripe's API uses, which matters directly for a Miami/US fintech audience.

- **How do you avoid deadlocks on concurrent transfers?**
  Transfers acquire `SELECT FOR UPDATE` locks on both accounts in a consistent order (by account ID), which is the standard technique to prevent circular wait conditions when two transfers touch the same pair of accounts in opposite directions.

- **What would you change for a production deployment?**
  Add authentication (JWT + Spring Security), wrap outbound calls in Resilience4j circuit breakers, stream audit events to Kafka for downstream consumers (fraud, reporting), and consider migrating read-heavy statement queries to optimistic locking or a read replica.

## Fit for target market

Directly demonstrates two patterns fintech recruiters in Miami/Austin look for: **client-supplied idempotency** (Stripe-style safe retries) and **double-entry accounting** (the same mental model used in ledger, wallet, and payments infrastructure at companies like Stripe, Chime, and traditional banks moving to microservices).
