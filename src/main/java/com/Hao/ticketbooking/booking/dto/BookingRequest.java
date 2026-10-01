package com.Hao.ticketbooking.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// The seats to confirm; the user must currently hold every one of them
public record BookingRequest(
        @NotNull
        Long eventId,

        @NotEmpty @Size(min = 1, max = 6)
        List<@NotNull Long> seatIds
) {
}
