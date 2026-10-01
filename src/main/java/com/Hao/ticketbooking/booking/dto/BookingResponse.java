package com.Hao.ticketbooking.booking.dto;

import com.Hao.ticketbooking.booking.BookingStatus;

import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        Long eventId,
        List<Long> seatIds,
        int totalCents,
        BookingStatus status,
        Instant createdAt
) {
}
