package com.Hao.ticketbooking.booking;

import com.Hao.ticketbooking.event.Seat;
import jakarta.persistence.*;

// One seat inside a booking. The partial unique index on (seat_id) WHERE status = 'CONFIRMED'
// is what stops the same seat being confirmed in two bookings.
@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @EmbeddedId
    private BookingSeatId id = new BookingSeatId();

    // @MapsId: this relationship IS the bookingId part of the key, so the two can't disagree
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("seatId")
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    protected BookingSeat() {
        // for JPA only
    }

    // Created only through Booking.addSeat, so a BookingSeat always belongs to a booking
    BookingSeat(Booking booking, Seat seat) {
        this.booking = booking;
        this.seat = seat;
        this.status = BookingStatus.CONFIRMED;
    }

    // Only called through Booking.cancel, so a booking and its seats always change together
    void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public BookingSeatId getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Seat getSeat() {
        return seat;
    }

    public BookingStatus getStatus() {
        return status;
    }
}
