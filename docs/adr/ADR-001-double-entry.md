# ADR-001: Double-Entry Accounting for Transaction Records

## Status
Accepted

## Date
2026-06-13

## Context
The system needs to record financial transactions in a way that is auditable, consistent, and mathematically verifiable. A naive approach would store only the resulting balance change on the affected account. However, this makes it impossible to reconstruct the full history of how a balance reached its current state, and provides no way to detect data corruption or bugs in balance calculation logic.

## Decision
We implement **double-entry accounting**: every transaction produces exactly two `TransactionEntry` records — one DEBIT and one CREDIT. Each entry stores:

- The account involved
- The entry type (DEBIT or CREDIT)
- The amount
- A snapshot of `balance_before` and `balance_after`

This means every financial event has two sides that must balance. The sum of all CREDIT entries minus the sum of all DEBIT entries for an account must equal the current balance.

## Alternatives Considered

### Single-entry recording
Store only the net balance change per transaction. Simpler to implement, but:
- Cannot reconstruct the full audit trail
- No mathematical invariant to verify correctness
- Industry standard for financial systems is double-entry

### Event sourcing
Derive balance from replaying all events. More powerful but:
- Significantly higher complexity
- Requires CQRS infrastructure
- Out of scope for this service's current requirements

## Consequences

**Positive:**
- Every transaction is mathematically verifiable: `SUM(CREDIT) - SUM(DEBIT) = balance`
- Full audit trail: `balance_before` and `balance_after` snapshots on every entry
- Corruption detection: inconsistent entries are detectable programmatically
- Aligns with GAAP and standard accounting practices, making the system understandable to finance stakeholders

**Negative:**
- Two DB writes per transaction instead of one
- Slightly more complex query logic for statement generation
- Storage overhead (roughly 2x entries vs single-entry)

## References
- [Double-entry bookkeeping — Wikipedia](https://en.wikipedia.org/wiki/Double-entry_bookkeeping)
- Martin Fowler — Accounting Patterns
