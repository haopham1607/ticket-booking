package com.Hao.ticketbooking.booking;

// Bookings can't be cancelled once the event has started → 409
public class EventAlreadyStartedException extends RuntimeException {

    public EventAlreadyStartedException(Long bookingId) {
        super("Event already started for booking " + bookingId);
    }
}
