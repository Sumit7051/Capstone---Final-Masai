# Shadow Ledger System - API Testing Guide (Insomnia)

## Prerequisites
Ensure all services are running:
1. **Docker**: Kafka, Zookeeper, PostgreSQL on ports 29092, 2181, 5432
2. **Event Service**: Port 8081
3. **Shadow Ledger Service**: Port 8082
4. **Drift Correction Service**: Port 8083
5. **API Gateway**: Port 8080

---

## 🚀 START SERVICES IN ORDER:

### Terminal 1: Event Service
```bash
cd C:\shadow-ledger-system-main/event-service
./mvnw spring-boot:run
```

### Terminal 2: Shadow Ledger Service
```bash
cd C:\shadow-ledger-system-main/shadow-ledger-service
./mvnw spring-boot:run
```

### Terminal 3: Drift Correction Service
```bash
cd C:\shadow-ledger-system-main/drift-correction-service
./mvnw spring-boot:run
```

### Terminal 4: API Gateway
```bash
cd C:\shadow-ledger-system-main/api-gateway
./mvnw spring-boot:run
```

---

## 📋 INSOMNIA TEST REQUESTS

### 1️⃣ HEALTH CHECKS (GET Requests)

#### Event Service Health
```
GET http://localhost:8081/actuator/health
```

#### Shadow Ledger Service Health
```
GET http://localhost:8082/actuator/health
```

#### Drift Correction Service Health
```
GET http://localhost:8083/actuator/health
```

#### API Gateway Health
```
GET http://localhost:8080/actuator/health
```

---

### 2️⃣ POST EVENTS (Event Service)

#### POST Event - Credit Transaction
```
POST http://localhost:8081/events  
Content-Type: application/json

json
{
  "eventId": "EVT-501",
  "accountId": "B21",
  "type": "credit",
  "amount": 1200,
  "timestamp": 1736000000000
}
```

#### POST Event - Another Credit
```
POST http://localhost:8081/events
Content-Type: application/json

{
  "eventId": "EVT-502",
  "accountId": "B21",
  "type": "credit",
  "amount": 400,
  "timestamp": 1736000100000
}
```

#### POST Event - Debit Transaction
```
POST http://localhost:8081/events
Content-Type: application/json

{
  "eventId": "EVT-503",
  "accountId": "B21",
  "type": "debit",
  "amount": 250,
  "timestamp": 1736000200000
}
```

#### POST Event - Different Account (B22)
```
POST http://localhost:8081/events
Content-Type: application/json

{
  "eventId": "EVT-601",
  "accountId": "B22",
  "type": "credit",
  "amount": 2000,
  "timestamp": 1736000300000
}

```

#### POST Event - Another for B22
```
POST http://localhost:8081/events
Content-Type: application/json

{
  "eventId": "EVT-602",
  "accountId": "B22",
  "type": "debit",
  "amount": 300,
  "timestamp": 1736000400000
}

```

**Expected Response:** `202 Accepted`

---

### 3️⃣ GET SHADOW BALANCE (Shadow Ledger Service)

**WAIT 5-10 seconds** after posting events for Kafka to process them!

#### Get Shadow Balance for Account B21
```
GET http://localhost:8082/accounts/B21/shadow-balance
```

**Expected Response:**
```json
{
  "accountId": "B21",
  "balance": 1350.0,
  "lastEvent": "EVT-503"
}

```

#### 
```
GET http://localhost:8082/accounts/B22/shadow-balance
```

**Expected Response:**
```json
{
  "accountId": "B22",
  "balance": 1700.0,
  "lastEvent": "EVT-602"
}

```

#### Get Shadow Balance for Non-Existent Account
```
GET http://localhost:8082/accounts/B99/shadow-balance
```

**Expected Response:**
```json
{
  "accountId": "B99",
  "balance": 0.0,
  "lastEvent": null
}

```

---

### 4️⃣ DRIFT CHECK (Drift Correction Service)

#### Check Drift - No Mismatch
```
POST http://localhost:8083/drift-check
Content-Type: application/json

[
  { "accountId": "B21", "reportedBalance": 1350.0 },
  { "accountId": "B22", "reportedBalance": 1700.0 }
]

```

**Expected Response:** `[]` (empty array - no corrections needed)

#### Check Drift - With Mismatch
```
POST http://localhost:8083/drift-check
Content-Type: application/json

[
  { "accountId": "B21", "reportedBalance": 1400.0 },
  { "accountId": "B22", "reportedBalance": 1600.0 }
]
```

**Expected Response:**
```json
[
  {
    "eventId": "CORR-B21-xxxxx",
    "accountId": "B21",
    "type": "credit",
    "amount": 50.0
  },
  {
    "eventId": "CORR-B22-xxxxx",
    "accountId": "B22",
    "type": "debit",
    "amount": 100.0
  }
]

```

---

### 5️⃣ MANUAL CORRECTION (Drift Correction Service)

#### Manual Correction - Add Credit to Account B21
```
POST http://localhost:8083/correct/B21?type=credit&amount=200
```

**Expected Response:**
```json
{
  "eventId": "MANUAL-B21-xxxxx",
  "accountId": "B21",
  "type": "credit",
  "amount": 200.0
}

```

#### Manual Correction - Add Debit to Account B22
```
POST http://localhost:8083/correct/A11?type=debit&amount=75
```

**Expected Response:**
```json
{
  "eventId": "MANUAL-B22-xxxxx",
  "accountId": "B22",
  "type": "debit",
  "amount": 75.0
}

```

**Note:** After manual corrections, wait 5-10 seconds and check shadow balance again to see the updated balance!

---

### 6️⃣ VIA API GATEWAY (with Security) - BONUS

#### Get JWT Token 
```
POST http://localhost:8080/auth/token
Content-Type: application/json

{
  "username": "testuser",
  "role": "USER"
}
```

#### POST Event via Gateway (requires JWT)
```
POST http://localhost:8080/events
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "eventId": "EVT-701",
  "accountId": "B23",
  "type": "credit",
  "amount": 900,
  "timestamp": 1736000500000
}

```

#### Check Drift via Gateway (AUDITOR role)
```
POST http://localhost:8080/drift-check
Content-Type: application/json
Authorization: Bearer <auditor-jwt-token>

[
  {
    "accountId": "B99",
    "reportedBalance": 850.0
  }
]
```

#### Manual Correction via Gateway (ADMIN role)
```
POST http://localhost:8080/correct/A10?type=credit&amount=50
Authorization: Bearer <admin-jwt-token>
```

---


## ❌ ERROR SCENARIOS TO TEST

### Duplicate Event ID
```
Error Scenarios

Duplicate Event

{
  "eventId": "EVT-501",
  "accountId": "B21",
  "type": "credit",
  "amount": 100
}


Invalid Type

{
  "eventId": "EVT-999",
  "accountId": "B21",
  "type": "bonus",
  "amount": 100
}


Negative Amount

{
  "eventId": "EVT-998",
  "accountId": "B21",
  "type": "credit",
  "amount": -500
}
```
**Expected:** Validation error (amount must be > 0)

---

## 🔍 MONITORING & DEBUGGING

### Check Kafka Topics
```bash
# If you have Kafka UI running on port 18081
http://localhost:18081
```

### View Database
```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres -d postgres

# Check shadow balance calculation
SELECT * FROM ledger_events ORDER BY timestamp;

SELECT account_id,
SUM(CASE WHEN type='CREDIT' THEN amount ELSE -amount END) AS balance
FROM ledger_events
GROUP BY account_id;

```

---

## 💡 TIPS

1. **Order matters**: Always post events before checking shadow balance
2. **Wait for Kafka**: Give 5-10 seconds between posting and querying
3. **Check logs**: If responses are unexpected, check service logs
4. **Event IDs must be unique**: Each eventId can only be used once
5. **Timestamps should increase**: Use increasing timestamps for proper ordering

---

## ✅ SUCCESS INDICATORS

-All services are healthy

-Events are processed correctly

-Shadow balances are accurate

-Drift detection works correctly

-Manual corrections are applied

-Invalid inputs are rejected


