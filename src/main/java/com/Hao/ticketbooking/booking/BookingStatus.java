package com.Hao.ticketbooking.booking;

// Must match the CHECK constraint on bookings.status and booking_seats.status
public enum BookingStatus {
    CONFIRMED,
    CANCELLED
}
