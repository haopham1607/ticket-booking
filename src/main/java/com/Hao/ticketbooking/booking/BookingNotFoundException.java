package com.Hao.ticketbooking.booking;

// The booking doesn't exist, or it belongs to another user. Both cases → 404,
// so the response never reveals whether someone else's booking exists.
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long id) {
        super("Booking not found: " + id);
    }
}
