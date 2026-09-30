package com.Hao.ticketbooking.hold;

import java.util.List;

// The one place the Redis key format for seat holds is defined: hold:{eventId}:{seatId}.
// Holding, releasing and the seat map must all use exactly the same keys.
public final class HoldKeys {

    private HoldKeys() {
    }

    public static String of(Long eventId, Long seatId) {
        return "hold:" + eventId + ":" + seatId;
    }

    public static List<String> of(Long eventId, List<Long> seatIds) {
        return seatIds.stream().map(seatId -> of(eventId, seatId)).toList();
    }
}
