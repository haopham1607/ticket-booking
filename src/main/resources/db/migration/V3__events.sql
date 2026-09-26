-- An event (a concert or show). Money is stored as integer cents, never as a float.
CREATE TABLE events (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    venue       VARCHAR(200) NOT NULL,
    starts_at   TIMESTAMPTZ  NOT NULL,
    price_cents INT          NOT NULL CHECK (price_cents >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One row per physical seat per event. Deleting an event deletes its seats.
CREATE TABLE seats (
    id        BIGSERIAL PRIMARY KEY,
    event_id  BIGINT      NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    row_label VARCHAR(5)  NOT NULL,
    number    INT         NOT NULL,
    UNIQUE (event_id, row_label, number)
);

-- One row per purchase. Cancelling changes the status; rows are never deleted.
CREATE TABLE bookings (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    event_id    BIGINT      NOT NULL REFERENCES events(id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- "My bookings" looks bookings up by user; Postgres doesn't index foreign keys on its own.
CREATE INDEX bookings_user_id_idx ON bookings (user_id);

-- One row per seat inside a purchase.
CREATE TABLE booking_seats (
    booking_id  BIGINT      NOT NULL REFERENCES bookings(id),
    seat_id     BIGINT      NOT NULL REFERENCES seats(id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    PRIMARY KEY (booking_id, seat_id)
);

-- The core guarantee: a seat can have at most one CONFIRMED booking.
-- Cancelled rows are ignored, so a cancelled seat can be booked again.
CREATE UNIQUE INDEX one_confirmed_booking_per_seat
    ON booking_seats (seat_id)
    WHERE status = 'CONFIRMED';
