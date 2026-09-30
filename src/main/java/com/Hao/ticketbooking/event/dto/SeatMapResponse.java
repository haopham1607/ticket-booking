package com.Hao.ticketbooking.event.dto;

import java.util.List;

// Seats grouped by row, in order: row A (seats 1..n), row B, ...
public record SeatMapResponse(Long eventId, List<RowResponse> rows) {
}
