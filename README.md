# Car Park Management API

A REST API for allocating parking spaces, tracking occupancy, 
billing and de-allocation vehicles on exit.

## Tech stack

- Java 25
- Spring Boot 4.1.0
- Gradle (Groovy DSL)
- Lombok
- In-memory storage only - no database

## Running the application

```bash
./gradlew bootRun
```

The API starts on **http://localhost:8080**.

## Running the tests

```bash
./gradlew test
```

## Building a jar

```bash
./gradlew clean build
java -jar build/libs/carpark-0.0.1-SNAPSHOT.jar
```

## API

### `GET /parking`

Returns current space availability.

```bash
curl http://localhost:8080/parking
```

```json
{ "availableSpaces": 50, "occupiedSpaces": 0 }
```

### `POST /parking`

Parks a vehicle in the first available space.

```bash
curl -X POST http://localhost:8080/parking \
  -H "Content-Type: application/json" \
  -d '{"vehicleReg": "AB12CDE", "vehicleType": 1}'
```

```json
{ "vehicleReg": "AB12CDE", "spaceNumber": 1, "timeIn": "2026-08-10T10:00:00" }
```

### `POST /parking/bill`

Frees the vehicle's space and returns its final charge.

```bash
curl -X POST http://localhost:8080/parking/bill \
  -H "Content-Type: application/json" \
  -d '{"vehicleReg": "AB12CDE"}'
```

```json
{
  "billId": "3f2b6b0a-9c1e-4c2a-8e3a-1a2b3c4d5e6f",
  "vehicleReg": "AB12CDE",
  "vehicleCharge": 1.5,
  "timeIn": "2026-08-10T10:00:00",
  "timeOut": "2026-08-10T10:05:00"
}
```

## Error handling

| Scenario                                           | Status           |
|----------------------------------------------------|------------------|
| Blank `vehicleReg`, or `vehicleType` not in 1-3    | 400 Bad Request  |
| Malformed JSON body                                | 400 Bad Request  |
| Parking a registration that's already parked       | 409 Conflict     |
| No available spaces                                | 409 Conflict     |
| Billing a registration that isn't currently parked | 404 Not Found    |

Every error returns the same shape:

```json
{ "message": "...", "timestamp": "2026-08-09T10:00:00" }
```

## Assumptions

1. **Architecture**: - a simple layered structure
   (controller → service → repository) to keep is simple.
2. `application.yml` holds a few configurable parameters:
   - **Total capacity** via `parking.totalSpaces` (**50** by default)
   - **Surcharge interval** via `parking.surcharge.interval` (**5 minutes** by default)
   - **Surcharge fee per interval** via `parking.surcharge.feePerInterval` (**£1** by default)
3. **The base charge** is billed on whole elapsed minutes (not rounded up).
4. **The £1 surcharge "every 5 minutes"** is applied once per **complete**
   5-minute block of parking time (not rounded up). 
   E.g. 12 minutes parked = 2 complete blocks = £2 surcharge (not £3).
5. **Minimum charge**: a stay under a minute is billed as 1 minute (there's never a £0 bill).
6. **`vehicleReg` is case-insensitive** (trimmed and upper-cased internally).
   E.g. `"ab12cde"` and `"AB12CDE"` are treated as the same vehicle.
7. **`billId`** is a randomly generated UUID.
8. **HTTP status codes** as the closest fit to REST conventions:
   - 400 for invalid input, 
   - 404 for an unknown vehicle on billing, 
   - 409 for conflicts (duplicated registration, full car park)
9. **Concurrency**: allocation and billing are synchronized in `ParkingService`, 
   since two simultaneous requests could race for the same space.
10. **Forget on exit** - there is no billing/registration history.
11. **Space number** - parking spaces are numbered starting from 1 up to the value of **`totalSpaces`**. 

## Questions

- Is car park capacity fixed, or should it be configurable/settable through the API?
- Should `vehicleReg` be validated against a real-world plate format, or is any non-blank string acceptable?
- Should there be a way to look up a currently parked vehicle (e.g.`GET /parking/{vehicleReg}`) without billing it?
- Should the 5-minute surcharge be applied for each started or completed block/interval?
- Is there any need to keep a billing/registration history after vehicle leaves the car park?

## Project structure

```
src/main/java/com/example/carpark/
├── CarparkApplication.java
├── config/       # configuration
├── controller/   # REST layer - request, response mapping only
├── dto/          # request, response records
├── exception/    # domain exceptions + global error handling
├── model/        # domain, value objects
├── repository/   # data access, no business rules
└── service/      # business rules - orchestrates allocation, billing, charge calculation
```

> **Note**:
> `ParkingService` orchestrates business rules and owns concurrency control.
> `InMemoryParkingRepository` only stores and looks up data.
