package com.Hao.ticketbooking.hold.dto;

import java.time.Instant;
import java.util.List;

// expiresAt lets a client show a countdown until the hold runs out
public record HoldResponse(Long eventId, List<Long> seatIds, Instant expiresAt) {
}
