# Rewards API

A Spring Boot REST API that calculates reward points for a retailer's customers, per month and in total,
over a three-month period.

## Reward rules

For every transaction:

- **2 points** for every dollar spent over $100
- **1 point** for every dollar spent between $50 and $100

Example: a $120 purchase earns `2 x 20 + 1 x 50 = 90` points.

A few decisions worth calling out:

| Amount   | Points | Why                                                    |
|----------|--------|--------------------------------------------------------|
| $50.00   | 0      | Nothing is spent *over* $50 yet                        |
| $100.00  | 50     | The full $50–$100 band counts                          |
| $101.00  | 52     | $1 over $100 (2 points) + the $50–$100 band (50 points) |
| $120.75  | 90     | Only whole dollars count, cents are dropped            |

## Reward period

The period is the **current month plus the two months before it**, ending today.
If today is 2026-10-24, the period is `2026-08-01` to `2026-10-24` and the months are
`2026-08`, `2026-09` and `2026-10`.

The period is calculated in the service and passed to the repository as a date range,
so older transactions are excluded even though they are still stored. Every month in the
period is always returned, with `0` points for months without purchases.

The number of months can be changed in `application.properties`:

```properties
rewards.period-months=3
```

## Tech stack

- Java 17
- Spring Boot 3.5 (Web)
- JUnit 5, Mockito, AssertJ, MockMvc
- Maven (wrapper included, no local Maven install needed)

No database is used. Data is kept in memory and loaded on startup.

## Running the app

Requires JDK 17 or newer.

```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Or build a jar and run it:

```bash
./mvnw clean package
java -jar target/rewards-api-1.0.0.jar
```

The API starts on `http://localhost:8080`.

## Running the tests

```bash
./mvnw test
```

| Test class                   | What it covers                                                      |
|------------------------------|---------------------------------------------------------------------|
| `RewardCalculatorTest`       | Points rules, including the $50 / $100 boundaries and cents         |
| `RewardServiceTest`          | Monthly and total points, period filtering, year rollover (fixed clock) |
| `TransactionServiceTest`     | Transaction listing and customer filtering                          |
| `RewardControllerTest`       | `/api/rewards` response shape and error responses                   |
| `TransactionControllerTest`  | `/api/transactions` response shape and error responses              |
| `RewardsApiIntegrationTest`  | End-to-end regression check of all endpoints against the sample data |

## Endpoints

### GET /api/rewards

Rewards for all customers.

```bash
curl http://localhost:8080/api/rewards
```

### GET /api/rewards/{customerId}

Rewards for a single customer.

```bash
curl http://localhost:8080/api/rewards/1
```

```json
{
  "customerId": 1,
  "customerName": "Emma Wilson",
  "periodStartDate": "2026-08-01",
  "periodEndDate": "2026-10-24",
  "monthlyRewards": [
    { "month": "2026-08", "points": 115 },
    { "month": "2026-09", "points": 50 },
    { "month": "2026-10", "points": 270 }
  ],
  "totalPoints": 435
}
```

### GET /api/transactions

All transactions, oldest first, with the points each one earned. Add `customerId` to filter
by customer.

```bash
curl http://localhost:8080/api/transactions
curl "http://localhost:8080/api/transactions?customerId=2"
```

```json
[
  {
    "transactionId": 7,
    "customerId": 2,
    "amount": 50.00,
    "transactionDate": "2026-08-10",
    "rewardPoints": 0
  }
]
```

### Errors

Errors use the standard Problem Details format (RFC 9457).

| Case                         | Status |
|------------------------------|--------|
| Customer id does not exist   | 404    |
| Customer id is not a number  | 400    |

```json
{
  "type": "about:blank",
  "title": "Customer not found",
  "status": 404,
  "detail": "Customer not found with id 999",
  "instance": "/api/rewards/999"
}
```

## Sample data

`SampleDataLoader` loads 4 customers and 15 transactions on startup. Dates are relative to the
current month, so the data always falls inside the reward period whenever the app is started.

| Customer         | Covers                                                        | Total points |
|------------------|---------------------------------------------------------------|--------------|
| 1 - Emma Wilson  | $100 boundary, a purchase under $50, one purchase that is too old | 435      |
| 2 - Liam Carter  | Exactly $50, just over $100, amount with cents                | 517          |
| 3 - Sophia Patel | No purchases in the oldest month                              | 199          |
| 4 - Noah Brooks  | Only purchases older than the period                          | 0            |

## Project structure

```
src/main/java/com/retailer/rewards
├── RewardsApplication.java
├── config        ClockConfig - system clock bean, replaced with a fixed clock in tests
├── controller    RewardController, TransactionController
├── data          SampleDataLoader - demo data loaded on startup
├── dto           API response records
├── exception     CustomerNotFoundException, GlobalExceptionHandler
├── model         Customer, Transaction
├── repository    In-memory customer and transaction repositories
└── service       RewardCalculator, RewardService, TransactionService
```
