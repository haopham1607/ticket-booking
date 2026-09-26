package com.Hao.ticketbooking.event.dto;

import com.Hao.ticketbooking.event.Event;

import java.time.Instant;

public record EventResponse(
        Long id,
        String name,
        String venue,
        Instant startsAt,
        int priceCents,
        long totalSeats
) {

    public static EventResponse from(Event event, long totalSeats) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getStartsAt(),
                event.getPriceCents(),
                totalSeats
        );
    }
}
