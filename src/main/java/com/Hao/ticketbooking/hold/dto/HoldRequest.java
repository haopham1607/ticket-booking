package com.Hao.ticketbooking.hold.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HoldRequest(
        @NotNull
        Long eventId,

        // 1 to 6 seats; @NotNull inside the brackets checks every element
        @NotEmpty @Size(min = 1, max = 6)
        List<@NotNull Long> seatIds
) {
}
