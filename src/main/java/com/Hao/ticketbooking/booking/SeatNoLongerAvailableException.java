package com.Hao.ticketbooking.booking;

// The partial unique index rejected the booking: another booking confirmed a seat first → 409.
// An expected outcome under concurrency, not a server error.
public class SeatNoLongerAvailableException extends RuntimeException {

    public SeatNoLongerAvailableException(Throwable cause) {
        super("Seat already confirmed in another booking", cause);
    }
}
