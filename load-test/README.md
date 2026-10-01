# Concurrency test

`race.py` releases many users at the same instant to fight for a handful of seats, then
checks the **database** for the invariants that must hold if the system is correct.

## Run it

The app, Postgres and Redis must be running (`docker compose up -d`, then start the app).

```bash
cd load-test
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python race.py                      # 500 users, 10 seats, http://localhost:8080
```

Options: `--users`, `--seats`, `--max-seats-per-user`, `--base-url`, `--keep` (keep the test
event and bookings), `--purge-users` (delete the `racer-*@loadtest.local` accounts afterwards).
The exit code is 0 when every invariant holds and 1 otherwise.

## What it does

1. Registers and logs in N users (`racer-{i}@loadtest.local`, reused between runs).
2. Creates an event with only a few seats, as the admin.
3. Releases every user at once. Each one tries to hold 1–2 random seats and, if the hold
   succeeds, confirms the booking.
4. Reports holds and bookings by outcome, plus latency.
5. Checks the database:

| Invariant | Query |
|---|---|
| No seat has more than one CONFIRMED booking | `GROUP BY seat_id HAVING COUNT(*) > 1` returns no rows |
| Confirmed seats ≤ seats in the event | count of CONFIRMED `booking_seats` rows |
| The API's answers match what was stored | 201 responses vs. rows in `bookings` / `booking_seats` |
| No 5xx responses | counted from the responses |

Checking the database matters: a bug could answer 409 and still write a row.

## Results

Measured on a single app instance on a laptop (11 cores), Postgres 16 and Redis 7 in Docker,
default settings (10 database connections).

### 500 users, 10 seats

| Run | Holds succeeded | Bookings | Seats sold | Double bookings | 5xx | Race duration |
|---|---|---|---|---|---|---|
| 1 (cold JVM) | 9 | 9 | 10 | 0 | 0 | 4.18 s |
| 2 | 9 | 9 | 10 | 0 | 0 | 1.02 s |
| 3 | 7 | 7 | 10 | 0 | 0 | 0.98 s |
| 4 | 8 | 8 | 10 | 0 | 0 | 1.02 s |
| 5 | 9 | 9 | 10 | 0 | 0 | 0.99 s |
| 6 | 8 | 8 | 10 | 0 | 0 | 0.99 s |

Every run sold exactly 10 seats to 7–9 users (some bought two), rejected the other ~491
with a clean 409, and passed all four invariants. A 200-user, 3-seat run behaved the same way.

Latency on warm runs: hold p50 ≈ 540 ms, p99 ≈ 950 ms. The hold endpoint runs two SQL queries
per request, and 500 simultaneous requests queue for the 10 database connections. That
queueing, not Redis, is where the time goes.

### Broken on purpose

The `experiment/break-it` branch removes the "is this seat already held?" check from
`hold_seats.lua`, so every hold succeeds and overwrites the previous one.

| Setup | Holds "succeeded" | Seats sold (of 10) | Double bookings | Result |
|---|---|---|---|---|
| **Both layers** (main) | 7–9 | 10 | 0 in 8 runs | correct |
| **Hold check removed**, unique index in place | 86–129 | 10 | 0 in 2 runs | correct: the confirm-time hold check and the unique index reject the extras (2 inserts were refused by the index itself) |
| **Hold check removed and unique index dropped** | similar | **11** in 2 of 4 runs | **1 seat sold twice** in 2 of 4 runs | **wrong** |

With the Redis layer broken, far more requests reach the database, and the partial unique
index is what keeps the result correct. With both gone, a seat is sold twice in about half
the runs. The failure is intermittent, which is exactly why this kind of bug survives manual
testing.

To reproduce the last row: check out `experiment/break-it`, run
`DROP INDEX one_confirmed_booking_per_seat;`, run the test, then recreate the index from
`V3__events.sql`. Don't do this with a migration, or Flyway will record it.
