# Customer Rewards Service

A Spring Boot REST service for a retailer's rewards program. It calculates the reward points every customer
earned **per month** and **in total** from their recorded purchases.

For the architecture, request-flow sequence diagrams, caching strategy and the reasoning behind the design
decisions, see [docs/DESIGN.md](docs/DESIGN.md).

## Reward rules

For each transaction:

| Spend in the transaction | Points |
|---|---|
| Every whole dollar **over $100** | 2 points |
| Every whole dollar **between $50 and $100** | 1 point |
| Up to $50 | 0 points |

Example: a **$120** purchase = 2 x $20 + 1 x $50 = **90 points**.

Notes on edge cases (all covered by unit tests):

- Only whole dollars count, cents are truncated: $100.99 earns 50 points, $50.99 earns 0.
- Exactly $50 earns 0; exactly $100 earns 50.
- Points are calculated per transaction, never on a monthly sum.

## Tech stack

| Concern | Choice |
|---|---|
| Framework | Spring Boot 4.1.1, Spring MVC, Java 17 |
| Database | HSQLDB, in-memory (seeded on startup) |
| Data access | DAO interfaces implemented with Spring `JdbcTemplate` |
| Caching | Spring Cache abstraction + Caffeine (in the service layer) |
| API docs | springdoc-openapi 3.1.0 (Swagger UI) |
| Boilerplate | Lombok |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc, `@JdbcTest` |

## Prerequisites

- **JDK 17 or newer** (the project targets Java 17; Java 8 will not work). Check with `java -version`.
- Nothing else. Maven is provided by the wrapper (`./mvnw`), and the database is embedded.

If several JDKs are installed, point `JAVA_HOME` at a 17+ JDK first, e.g. on macOS:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

## Build, test, start and stop

```bash
# run the unit + integration tests
./mvnw test

# build the executable jar (target/customer-rewards-service-1.0.0-SNAPSHOT.jar)
./mvnw clean package

# start - option 1: straight from Maven
./mvnw spring-boot:run

# start - option 2: the packaged jar
java -jar target/customer-rewards-service-1.0.0-SNAPSHOT.jar
```

The service listens on **http://localhost:8080** (change with `--server.port=9090`).

**Stop:** press `Ctrl+C` in the terminal that runs it. If it was started in the background:

```bash
kill $(lsof -ti :8080)
```

The database is in memory: it is recreated and re-seeded from `schema.sql` / `data.sql` on each start, and anything
posted through the API is lost on shutdown.

### Useful URLs

| URL | Purpose |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI (try the APIs in the browser) |
| http://localhost:8080/v3/api-docs | OpenAPI 3 document (JSON), also checked in at [docs/openapi.json](docs/openapi.json) |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/actuator/caches | Registered caches |

## API

Base path: `/api/v1`. All responses are JSON.

### Reporting period

The `from` and `to` query parameters (ISO dates, `yyyy-MM-dd`, both inclusive) are optional:

| Given | Period reported |
|---|---|
| `from` and `to` | exactly that period (max 366 days, `from` must not be after `to`) |
| only `to` | the 3 calendar months ending with the month of `to` |
| only `from` | the 3 calendar months starting with the month of `from` |
| neither | the 3 calendar months ending with the month of the **most recent transaction** |

The response always contains one entry per calendar month in the period, including months with no purchases (0 points).

### `GET /api/v1/rewards` - rewards of all customers

```bash
curl "http://localhost:8080/api/v1/rewards"
curl "http://localhost:8080/api/v1/rewards?from=2026-06-01&to=2026-08-31"
```

Returns a list with one entry per customer (customers without purchases are included with 0 points).

### `GET /api/v1/rewards/{customerId}` - rewards of one customer

```bash
curl "http://localhost:8080/api/v1/rewards/1"
```

```json
{
  "customerId": 1,
  "customerName": "Alice Johnson",
  "periodStart": "2026-06-01",
  "periodEnd": "2026-08-31",
  "monthlyRewards": [
    { "month": "2026-06", "transactionCount": 2, "points": 115 },
    { "month": "2026-07", "transactionCount": 2, "points": 250 },
    { "month": "2026-08", "transactionCount": 2, "points": 160 }
  ],
  "totalPoints": 525
}
```

Errors: `404` unknown customer, `400` invalid or malformed period.

### `POST /api/v1/transactions` - record a purchase

```bash
curl -X POST "http://localhost:8080/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -d '{"customerId": 5, "amount": 120.00, "transactionDate": "2026-08-20", "description": "Headphones"}'
```

| Field | Rules |
|---|---|
| `customerId` | required, must exist |
| `amount` | required, > 0, at most 2 decimal places |
| `transactionDate` | required, not in the future |
| `description` | optional, max 200 characters |

Returns `201 Created` with the stored transaction and the points it earned (`rewardPoints`). Errors: `400` validation
failed, `404` unknown customer. Recording a transaction clears the rewards caches.

### `GET /api/v1/transactions?customerId={id}` - list purchases of a customer

```bash
curl "http://localhost:8080/api/v1/transactions?customerId=1"
```

Each item includes the `rewardPoints` earned by that purchase. Errors: `404` unknown customer.

### `GET /api/v1/customers` - list customers

### Error format

Every error uses the same body:

```json
{
  "timestamp": "2026-09-21T21:32:55.546297Z",
  "status": 404,
  "error": "Not Found",
  "message": "Customer 99 not found",
  "path": "/api/v1/rewards/99",
  "details": null
}
```

Validation errors list the offending fields in `details`.

## Sample data

`src/main/resources/data.sql` seeds 5 customers and 20 transactions between May and August 2026, chosen to hit the
rule boundaries ($49.99, $50, $100, $100.01, ...). With the default period (June - August 2026):

| Customer | June | July | August | **Total** |
|---|---:|---:|---:|---:|
| 1 - Alice Johnson | 115 | 250 | 160 | **525** |
| 2 - Bob Smith | 52 | 350 | 50 | **452** |
| 3 - Carol Williams | 850 | 5 | 70 | **925** |
| 4 - David Brown | 0 | 99 | 200 | **299** |
| 5 - Eve Davis | 0 | 0 | 0 | **0** |

Alice also has a $300 purchase in May 2026, which is outside the default period. Ask for it explicitly with
`/api/v1/rewards/1?from=2026-05-01&to=2026-05-31` (450 points).

## Caching

The service layer caches calculated rewards (Caffeine, evicted whenever a new purchase is recorded). See
[Caching design](docs/DESIGN.md#caching-design) in the design document for the cache keys, expiry and known limits.
