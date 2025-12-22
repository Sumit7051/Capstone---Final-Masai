# Correction Strategy - Shadow Ledger System

## Overview
The Drift Correction Service detects and corrects discrepancies between the Core Banking System (CBS) reported balances and the Shadow Ledger calculated balances.

## Drift Detection Process

### 1. CBS Balance Comparison
```
POST /drift-check
Body: [{"accountId": "A10", "reportedBalance": 700}]
```

Steps:
1. Receive CBS balance report
2. Query Shadow Ledger Service for current shadow balance
3. Calculate difference: `drift = CBS_balance - shadow_balance`
4. If drift ≠ 0, generate correction event

### 2. Drift Classification

#### No Drift (drift = 0)
**Action**: No correction needed  
**Response**: Empty correction list `[]`

#### Positive Drift (CBS > Shadow)
**Scenario**: CBS shows higher balance than shadow ledger  
**Likely Cause**: Missing credit transaction in shadow ledger  
**Correction**: Generate CREDIT event for difference amount

**Example:**
```
CBS Balance: 750
Shadow Balance: 700
Drift: +50
Correction: {"eventId": "CORR-A10-1", "type": "credit", "amount": 50}
```

#### Negative Drift (CBS < Shadow)
**Scenario**: CBS shows lower balance than shadow ledger  
**Likely Cause**: Missing debit transaction or incorrect credit  
**Correction**: Generate DEBIT event for absolute difference

**Example:**
```
CBS Balance: 650
Shadow Balance: 700
Drift: -50
Correction: {"eventId": "CORR-A10-2", "type": "debit", "amount": 50}
```

## Correction Event Generation

### Event ID Format
```
CORR-{accountId}-{uniqueId}
```

Example: `CORR-A10-a3f2b1c8`

### Correction Event Structure
```json
{
  "eventId": "CORR-A10-12345678",
  "accountId": "A10",
  "type": "credit" | "debit",
  "amount": 50.0
}
```

### Publishing to Kafka
All corrections are published to `transactions.corrections` topic, which is consumed by Shadow Ledger Service along with regular transactions.

## Manual Correction Endpoint

### Use Case
For corrections that require manual intervention or override.

### API
```
POST /correct/{accountId}?type=credit&amount=100
```

### Process
1. Validate account ID and parameters
2. Generate correction event with `MANUAL-` prefix
3. Publish to `transactions.corrections` topic
4. Return correction event details

### Event ID Format
```
MANUAL-{accountId}-{uniqueId}
```

Example: `MANUAL-A10-9f8e7d6c`

## Correction Processing

### Shadow Ledger Integration
The Shadow Ledger Service consumes corrections from two topics:
```java
@KafkaListener(topics = {"transactions.raw", "transactions.corrections"})
```

1. Corrections are treated as regular events
2. Applied to ledger with same ordering rules
3. Balance recalculated using window function
4. No special handling needed—corrections are just events

## Idempotency

### Duplicate Correction Prevention
- Each correction has unique event ID
- Deduplication happens at Shadow Ledger level
- Same correction won't be applied twice

### Re-running Drift Check
If drift check is run multiple times:
1. First run: Generates correction (e.g., +50 credit)
2. Correction applied to shadow ledger
3. Second run: No drift detected (balances match)
4. No additional correction generated

## Audit Trail

All corrections are:
✅ Logged in `ledger_events` table  
✅ Marked with `CORR-` or `MANUAL-` prefix  
✅ Traceable to original drift detection or manual request  
✅ Part of immutable ledger history  

