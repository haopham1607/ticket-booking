package com.Hao.ticketbooking.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

// Integer instead of int: a missing field becomes null and fails @NotNull,
// instead of silently becoming 0 (a free event, or zero seats).
public record CreateEventRequest(
        @NotBlank @Size(max = 200)
        String name,

        @NotBlank @Size(max = 200)
        String venue,

        @NotNull @Future
        Instant startsAt,

        @NotNull @PositiveOrZero
        Integer priceCents,

        // Rows are labelled A–Z
        @NotNull @Min(1) @Max(26)
        Integer rows,

        @NotNull @Min(1) @Max(100)
        Integer seatsPerRow
) {
}
