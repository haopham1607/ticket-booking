"""
Concurrency test: many users race for a handful of seats at the same moment.

It registers and logs in N test users, creates a small event as the admin, then releases
every user at once to hold and confirm seats. Afterwards it checks the DATABASE (not just
the HTTP responses) for the invariants that must hold if the system is correct:

  1. no seat has more than one CONFIRMED booking
  2. confirmed seats <= seats in the event
  3. what the API said matches what the database stored
  4. no 5xx responses

Usage:
    python race.py                        # 500 users, 10 seats, http://localhost:8080
    python race.py --users 200 --seats 5
    python race.py --keep                 # keep the test event and bookings for inspection
    python race.py --purge-users          # also delete the racer accounts afterwards

Exit code 0 = every invariant held, 1 = at least one failed.
"""

import argparse
import asyncio
import collections
import os
import random
import sys
import time
from datetime import datetime, timedelta, timezone

import httpx
import psycopg

PASSWORD = "racer-password-123"
EMAIL = "racer-{i}@loadtest.local"


def parse_args():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--base-url", default=os.getenv("BASE_URL", "http://localhost:8080"))
    p.add_argument("--users", type=int, default=500)
    p.add_argument("--seats", type=int, default=10)
    p.add_argument("--max-seats-per-user", type=int, default=2, help="each user asks for 1..N random seats")
    p.add_argument("--admin-email", default=os.getenv("ADMIN_EMAIL", "admin@gmail.com"))
    p.add_argument("--admin-password", default=os.getenv("ADMIN_PASSWORD", "admin123"))
    p.add_argument("--db", default=os.getenv(
        "DATABASE_URL", "postgresql://ticketbooking:ticketbooking@localhost:5432/ticketbooking"))
    p.add_argument("--keep", action="store_true", help="keep the test event and its bookings")
    p.add_argument("--purge-users", action="store_true", help="delete the racer accounts at the end")
    return p.parse_args()


async def login(client, email, password):
    r = await client.post("/api/auth/login", json={"email": email, "password": password})
    r.raise_for_status()
    return r.json()["token"]


async def prepare_user(client, limiter, i):
    """Register (409 = already exists from an earlier run, which is fine) and log in."""
    email = EMAIL.format(i=i)
    async with limiter:  # BCrypt is slow on purpose; don't overload the server during setup
        r = await client.post("/api/auth/register", json={"email": email, "password": PASSWORD})
        if r.status_code not in (201, 409):
            r.raise_for_status()
        return await login(client, email, PASSWORD)


async def create_event(client, admin_token, seats):
    starts_at = (datetime.now(timezone.utc) + timedelta(days=30)).isoformat()
    r = await client.post(
        "/api/events",
        headers={"Authorization": f"Bearer {admin_token}"},
        json={"name": f"Race test {datetime.now():%H:%M:%S}", "venue": "Load test hall",
              "startsAt": starts_at, "priceCents": 1000, "rows": 1, "seatsPerRow": seats})
    r.raise_for_status()
    event_id = r.json()["id"]
    seat_map = (await client.get(f"/api/events/{event_id}/seats")).json()
    seat_ids = [seat["id"] for row in seat_map["rows"] for seat in row["seats"]]
    return event_id, seat_ids


async def race_one(client, start, token, event_id, picks, results, latencies):
    """One user: wait for the starting gun, try to hold, and confirm if the hold succeeded."""
    headers = {"Authorization": f"Bearer {token}"}
    body = {"eventId": event_id, "seatIds": picks}
    await start.wait()

    t = time.perf_counter()
    hold = await client.post("/api/holds", headers=headers, json=body)
    latencies["hold"].append(time.perf_counter() - t)
    results["hold"][hold.status_code] += 1
    if hold.status_code != 200:
        return None

    t = time.perf_counter()
    booking = await client.post("/api/bookings", headers=headers, json=body)
    latencies["booking"].append(time.perf_counter() - t)
    key = booking.status_code if booking.status_code == 201 else (
        booking.status_code, booking.json().get("error", "?") if booking.content else "?")
    results["booking"][key] += 1
    return picks if booking.status_code == 201 else None


def percentile(values, p):
    if not values:
        return 0.0
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(len(ordered) * p))] * 1000


def check_invariants(conn, event_id, seat_count, api_bookings, api_seats, server_errors):
    """Ask the database what really happened. Returns a list of (name, passed, detail)."""
    doubles = conn.execute("""
        SELECT bs.seat_id, COUNT(*)
        FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id
        WHERE b.event_id = %s AND bs.status = 'CONFIRMED'
        GROUP BY bs.seat_id HAVING COUNT(*) > 1""", (event_id,)).fetchall()
    confirmed_rows = conn.execute("""
        SELECT COUNT(*) FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id
        WHERE b.event_id = %s AND bs.status = 'CONFIRMED'""", (event_id,)).fetchone()[0]
    distinct_seats = conn.execute("""
        SELECT COUNT(DISTINCT bs.seat_id) FROM booking_seats bs JOIN bookings b ON b.id = bs.booking_id
        WHERE b.event_id = %s AND bs.status = 'CONFIRMED'""", (event_id,)).fetchone()[0]
    db_bookings = conn.execute(
        "SELECT COUNT(*) FROM bookings WHERE event_id = %s AND status = 'CONFIRMED'", (event_id,)).fetchone()[0]

    return [
        ("no seat has more than one CONFIRMED booking", not doubles,
         "0 seats double-booked" if not doubles else f"{len(doubles)} seats double-booked: {doubles}"),
        (f"confirmed seats <= {seat_count} seats in the event", distinct_seats <= seat_count and confirmed_rows <= seat_count,
         f"{confirmed_rows} confirmed seat rows covering {distinct_seats} distinct seats"),
        ("API responses match the database", db_bookings == api_bookings and confirmed_rows == api_seats,
         f"API said {api_bookings} bookings / {api_seats} seats; database has {db_bookings} / {confirmed_rows}"),
        ("no 5xx responses", server_errors == 0, f"{server_errors} server errors"),
    ]


def cleanup(conn, event_id, purge_users):
    conn.execute("DELETE FROM booking_seats WHERE booking_id IN (SELECT id FROM bookings WHERE event_id = %s)", (event_id,))
    conn.execute("DELETE FROM bookings WHERE event_id = %s", (event_id,))
    conn.execute("DELETE FROM events WHERE id = %s", (event_id,))   # seats go with it (ON DELETE CASCADE)
    if purge_users:
        conn.execute("DELETE FROM users WHERE email LIKE 'racer-%%@loadtest.local'")
    conn.commit()


async def main():
    args = parse_args()
    limits = httpx.Limits(max_connections=args.users + 50, max_keepalive_connections=args.users + 50)
    async with httpx.AsyncClient(base_url=args.base_url, timeout=60, limits=limits) as client:
        print(f"Target: {args.base_url}   users: {args.users}   seats: {args.seats}")

        t = time.perf_counter()
        limiter = asyncio.Semaphore(16)
        tokens = await asyncio.gather(*(prepare_user(client, limiter, i) for i in range(args.users)))
        admin_token = await login(client, args.admin_email, args.admin_password)
        event_id, seat_ids = await create_event(client, admin_token, args.seats)
        print(f"Setup: {len(tokens)} users logged in, event {event_id} with {len(seat_ids)} seats "
              f"({time.perf_counter() - t:.1f}s)")

        results = {"hold": collections.Counter(), "booking": collections.Counter()}
        latencies = {"hold": [], "booking": []}
        start = asyncio.Event()
        tasks = [asyncio.create_task(race_one(
            client, start, token, event_id,
            random.sample(seat_ids, random.randint(1, min(args.max_seats_per_user, len(seat_ids)))),
            results, latencies)) for token in tokens]
        await asyncio.sleep(0.5)          # let every task reach the starting line
        t = time.perf_counter()
        start.set()                       # the starting gun: everyone goes at once
        outcomes = await asyncio.gather(*tasks)
        duration = time.perf_counter() - t

    won = [picks for picks in outcomes if picks]
    api_bookings, api_seats = len(won), sum(len(p) for p in won)
    server_errors = sum(n for kind in results.values() for key, n in kind.items()
                        if (key if isinstance(key, int) else key[0]) >= 500)

    print(f"\nRace: {args.users} users released at once, finished in {duration:.2f}s")
    print("  Holds")
    print(f"    succeeded (200)            {results['hold'][200]:5d}")
    for status, n in sorted((k, v) for k, v in results["hold"].items() if k != 200):
        print(f"    rejected  ({status})            {n:5d}")
    print("  Bookings")
    print(f"    succeeded (201)            {results['booking'][201]:5d}   -> {api_seats} seats sold")
    for key, n in sorted(((k, v) for k, v in results["booking"].items() if k != 201), key=str):
        print(f"    rejected  {str(key):28s} {n:5d}")
    print(f"  Latency  hold p50 {percentile(latencies['hold'], .5):.0f} ms, p99 {percentile(latencies['hold'], .99):.0f} ms"
          f"   booking p50 {percentile(latencies['booking'], .5):.0f} ms, p99 {percentile(latencies['booking'], .99):.0f} ms")

    with psycopg.connect(args.db) as conn:
        checks = check_invariants(conn, event_id, len(seat_ids), api_bookings, api_seats, server_errors)
        print("\nInvariants (checked in the database)")
        for name, passed, detail in checks:
            print(f"  [{'PASS' if passed else 'FAIL'}] {name}: {detail}")
        if args.keep:
            print(f"\nKept event {event_id} and its bookings (--keep)")
        else:
            cleanup(conn, event_id, args.purge_users)

    ok = all(passed for _, passed, _ in checks)
    print(f"\nRESULT: {'ALL INVARIANTS HELD' if ok else 'INVARIANT VIOLATED'}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
