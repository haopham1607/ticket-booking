package com.Hao.ticketbooking.hold.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReleaseRequest(
        @NotNull
        Long eventId,

        @NotEmpty @Size(min = 1, max = 6)
        List<@NotNull Long> seatIds
) {
}
