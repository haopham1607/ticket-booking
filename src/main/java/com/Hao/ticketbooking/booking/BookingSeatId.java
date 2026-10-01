package com.Hao.ticketbooking.booking;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

// The composite primary key of booking_seats: (booking_id, seat_id).
// JPA compares keys to tell rows apart, so equals() and hashCode() are required.
@Embeddable
public class BookingSeatId implements Serializable {

    private Long bookingId;
    private Long seatId;

    protected BookingSeatId() {
        // for JPA only
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getSeatId() {
        return seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingSeatId other)) return false;
        return Objects.equals(bookingId, other.bookingId) && Objects.equals(seatId, other.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, seatId);
    }
}
