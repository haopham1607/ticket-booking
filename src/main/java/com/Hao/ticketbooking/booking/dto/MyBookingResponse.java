package com.Hao.ticketbooking.booking.dto;

import com.Hao.ticketbooking.booking.Booking;
import com.Hao.ticketbooking.booking.BookingStatus;
import com.Hao.ticketbooking.event.Event;
import com.Hao.ticketbooking.event.Seat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

// One booking in "my bookings", with the event and seats a user needs to see
public record MyBookingResponse(
        Long bookingId,
        BookingStatus status,
        Instant createdAt,
        int totalCents,
        EventSummary event,
        List<SeatSummary> seats
) {

    public record EventSummary(Long id, String name, String venue, Instant startsAt) {
    }

    public record SeatSummary(Long id, String row, int number) {
    }

    // Must be called while the booking's event and seats are loaded (see the JOIN FETCH queries)
    public static MyBookingResponse from(Booking booking) {
        Event event = booking.getEvent();
        List<SeatSummary> seats = booking.getSeats().stream()
                .map(bookingSeat -> {
                    Seat seat = bookingSeat.getSeat();
                    return new SeatSummary(seat.getId(), seat.getRowLabel(), seat.getNumber());
                })
                .sorted(Comparator.comparing(SeatSummary::row).thenComparing(SeatSummary::number))
                .toList();
        return new MyBookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getCreatedAt(),
                event.getPriceCents() * seats.size(),
                new EventSummary(event.getId(), event.getName(), event.getVenue(), event.getStartsAt()),
                seats
        );
    }
}
