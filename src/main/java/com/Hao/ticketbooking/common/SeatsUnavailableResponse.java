package com.Hao.ticketbooking.common;

import java.util.List;

/**
 * The JSON body for a 409 when seats can't be held, e.g.
 * { "error": "SEATS_UNAVAILABLE", "message": "...", "seatIds": [217] }
 */
public record SeatsUnavailableResponse(String error, String message, List<Long> seatIds) {
}
