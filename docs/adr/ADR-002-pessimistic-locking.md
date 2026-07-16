# ADR-002: Pessimistic Locking for Account Transfers

## Status
Accepted

## Date
2026-06-13

## Context
Transfer operations read two account balances, validate that the source has sufficient funds, then update both balances atomically. Without concurrency control, two simultaneous transfers from the same account could both pass the balance check and together overdraft the account — a classic lost update problem.

## Decision
We use **pessimistic locking** (`SELECT FOR UPDATE`) on both accounts before executing a transfer. Locks are acquired in a **consistent order sorted by account UUID** to prevent deadlocks.

Implementation:

```java
// Lock ordering prevents deadlock
UUID firstId  = source.compareTo(destination) < 0 ? source : destination;
UUID secondId = source.compareTo(destination) < 0 ? destination : source;

Account first  = accountRepository.findByIdWithLock(firstId);
Account second = accountRepository.findByIdWithLock(secondId);
```

## Alternatives Considered

### Optimistic locking (`@Version`)
Add a `version` column to `Account`. On update, check that the version hasn't changed since read. If it has, retry.

Pros:
- Better throughput under low contention
- No lock held during processing

Cons:
- Requires retry logic in the application layer
- Under high contention (many transfers from same account), retry storms degrade performance
- More complex error handling

Pessimistic locking was chosen because financial transfer correctness is the priority over throughput at this stage.

### Serializable transaction isolation
Set `TRANSACTION_SERIALIZABLE` at the DB level.

Pros: no application-level locking needed
Cons: high abort rate under concurrency, significant performance impact, overkill for this use case

### Application-level distributed lock (Redis)
Use Redis `SETNX` to lock accounts by ID.

Pros: works across multiple service instances
Cons: introduces Redis as a required dependency, adds network latency, complex failure handling

## Consequences

**Positive:**
- Correctness guaranteed: no overdraft possible under concurrent load
- Simple implementation: Spring Data JPA `@Lock(PESSIMISTIC_WRITE)` annotation
- Deadlock-free: consistent lock ordering by UUID eliminates circular wait
- Verified by `TransferConcurrencyIT`: 10 concurrent threads, zero overdraft

**Negative:**
- Reduced throughput on hot accounts (many transfers from same source)
- Locks held for the duration of the transaction — long transactions are costly
- Does not scale across multiple service instances without a distributed lock or event-driven approach

## Future Migration Path
When throughput becomes a bottleneck, migrate to optimistic locking with retry, or adopt an event-driven architecture where balance updates are processed sequentially per account via Kafka partitioning.
