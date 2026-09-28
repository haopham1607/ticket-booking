package com.Hao.ticketbooking.hold;

import java.util.List;

// Seats already booked, or held by another user → 409, listing the seats
public class SeatsUnavailableException extends RuntimeException {

    private final List<Long> seatIds;

    public SeatsUnavailableException(List<Long> seatIds) {
        super("Seats not available: " + seatIds);
        this.seatIds = List.copyOf(seatIds);
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }
}
