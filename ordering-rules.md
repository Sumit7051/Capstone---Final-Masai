# Ordering Rules — Shadow Ledger System

Purpose
- Define how events are ordered, produced and consumed so account balances remain consistent across services.

Invariants
- Event IDs are globally unique and idempotent keys (eventId).
- Per-account ordering must be preserved: all events for a given account MUST be applied in the same order they were produced.
- Timestamps SHOULD be monotonic per-account (increasing), but consumers must handle late / corrective events.
- Corrections (automated or manual) are published as events and follow the same ordering/idempotency rules.

Producer rules
- Publish ledger events to the central Kafka topic (e.g. `ledger-events`) using `accountId` as the message key so Kafka guarantees partition ordering per account.
- Include these fields in every event:
  - eventId (string, unique)
  - accountId (string)
  - type (credit|debit)
  - amount (decimal)
  - timestamp (epoch ms)
  - optional: metadata/correlationId
- Prefer assigning a monotonic timestamp/sequence at producer side. If sequence is added, include sequenceNumber to aid strict ordering checks.

Kafka / partitioning
- Topic must have enough partitions for throughput, but ordering guarantee only holds within a partition.
- Use a consistent partitioner that hashes `accountId` → partition to guarantee per-account ordering.

Consumer rules (Shadow Ledger / Drift Correction)
- Process messages in the order delivered by Kafka for each partition.
- Maintain a small per-account state:
  - lastAppliedEventId
  - lastAppliedTimestamp (or sequenceNumber)
  - currentBalance
- Apply an incoming event only if:
  - eventId is not already applied (dedupe by eventId), AND
  - event.timestamp >= lastAppliedTimestamp (or sequenceNumber >= lastSequence) OR it is explicitly a correction event that must be allowed to adjust balance.
- For events older than lastAppliedTimestamp:
  - Prefer publishing a correction event rather than retroactively reordering already-applied events.
  - Alternatively, route to manual review / dead-letter if business rules forbid retroactive changes.

Idempotency & deduplication
- Persist a record of processed eventIds (or mark ledger_events rows uniquely) to prevent double-application.
- Database inserts for ledger events must enforce a unique constraint on event_id.

Corrections & Manual Adjustments
- Corrections are events (eventId prefixed like CORR-*, MANUAL-*) and must be treated as normal events by consumers.
- Drift Correction service should compute diff and publish corrective events keyed by accountId so ordering is preserved.

Out-of-order / late-arriving events
- Design policy up-front:
  - Soft policy: accept late events if idempotent and apply as adjustments (may change balance).
  - Strict policy: reject late events and require compensation/correction events.
- Implement dead-letter topic for events failing validation (duplicate ID, invalid type, negative amount).

Testing & verification
- Integration tests should:
  - Produce multiple events for one account and verify final balance.
  - Produce duplicate eventId and expect rejection or dedupe behavior.
  - Produce out-of-order timestamps and validate system reaction per policy.
- Examples (producer payload):
```json
{
  "eventId": "E1001",
  "accountId": "A10",
  "type": "credit",
  "amount": 500,
  "timestamp": 1735561800000
}
```

Operational notes
- If running services outside Docker network, use host:port for DBs; ensure Kafka hostname resolution is consistent with producers/consumers.
- Monitor per-partition lag to detect consumers falling behind and potential reordering due to rebalance.
- Ensure schema evolution is backward-compatible; use topic versioning or schema registry if required.

Keep ordering rules small and explicit for each service; update this file when the production policy (accept-late vs reject-late) changes.
```// filepath: 
C:\shadow-ledger-system-main/shadow-ledger-system-main\ordering-rules.md
# Ordering Rules — Shadow Ledger System

Purpose
- Define how events are ordered, produced and consumed so account balances remain consistent across services.

Invariants
- Event IDs are globally unique and idempotent keys (eventId).
- Per-account ordering must be preserved: all events for a given account MUST be applied in the same order they were produced.
- Timestamps SHOULD be monotonic per-account (increasing), but consumers must handle late / corrective events.
- Corrections (automated or manual) are published as events and follow the same ordering/idempotency rules.

Producer rules
- Publish ledger events to the central Kafka topic (e.g. `ledger-events`) using `accountId` as the message key so Kafka guarantees partition ordering per account.
- Include these fields in every event:
  - eventId (string, unique)
  - accountId (string)
  - type (credit|debit)
  - amount (decimal)
  - timestamp (epoch ms)
  - optional: metadata/correlationId
- Prefer assigning a monotonic timestamp/sequence at producer side. If sequence is added, include sequenceNumber to aid strict ordering checks.

Kafka / partitioning
- Topic must have enough partitions for throughput, but ordering guarantee only holds within a partition.
- Use a consistent partitioner that hashes `accountId` → partition to guarantee per-account ordering.

Consumer rules (Shadow Ledger / Drift Correction)
- Process messages in the order delivered by Kafka for each partition.
- Maintain a small per-account state:
  - lastAppliedEventId
  - lastAppliedTimestamp (or sequenceNumber)
  - currentBalance
- Apply an incoming event only if:
  - eventId is not already applied (dedupe by eventId), AND
  - event.timestamp >= lastAppliedTimestamp (or sequenceNumber >= lastSequence) OR it is explicitly a correction event that must be allowed to adjust balance.
- For events older than lastAppliedTimestamp:
  - Prefer publishing a correction event rather than retroactively reordering already-applied events.
  - Alternatively, route to manual review / dead-letter if business rules forbid retroactive changes.

Idempotency & deduplication
- Persist a record of processed eventIds (or mark ledger_events rows uniquely) to prevent double-application.
- Database inserts for ledger events must enforce a unique constraint on event_id.

Corrections & Manual Adjustments
- Corrections are events (eventId prefixed like CORR-*, MANUAL-*) and must be treated as normal events by consumers.
- Drift Correction service should compute diff and publish corrective events keyed by accountId so ordering is preserved.

Out-of-order / late-arriving events
- Design policy up-front:
  - Soft policy: accept late events if idempotent and apply as adjustments (may change balance).
  - Strict policy: reject late events and require compensation/correction events.
- Implement dead-letter topic for events failing validation (duplicate ID, invalid type, negative amount).

Testing & verification
- Integration tests should:
  - Produce multiple events for one account and verify final balance.
  - Produce duplicate eventId and expect rejection or dedupe behavior.
  - Produce out-of-order timestamps and validate system reaction per policy.
- Examples (producer payload):
```json
{
  "eventId": "E1001",
  "accountId": "A10",
  "type": "credit",
  "amount": 500,
  "timestamp": 1735561800000
}
```

Operational notes
- If running services outside Docker network, use host:port for DBs; ensure Kafka hostname resolution is consistent with producers/consumers.
- Monitor per-partition lag to detect consumers falling behind and potential reordering due to rebalance.
- Ensure schema evolution is backward-compatible; use topic versioning or schema registry if required.

Keep ordering rules small and explicit for each service; update this file when the production policy (accept-late vs reject-late) changes.