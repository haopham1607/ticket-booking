package com.Hao.ticketbooking.event;

import com.Hao.ticketbooking.event.dto.RowResponse;
import com.Hao.ticketbooking.event.dto.SeatMapResponse;
import com.Hao.ticketbooking.event.dto.SeatResponse;
import com.Hao.ticketbooking.event.dto.SeatStatus;
import com.Hao.ticketbooking.hold.HoldKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the seat map: every seat of an event as AVAILABLE, HELD or BOOKED.
 * One SQL query (seats + confirmed bookings) and one Redis MGET (holds), however many seats.
 * Not cached: holds change every few seconds, so a cached copy would be wrong almost immediately.
 */
@Service
@Transactional(readOnly = true)
public class SeatMapService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final StringRedisTemplate redis;

    public SeatMapService(EventRepository eventRepository, SeatRepository seatRepository, StringRedisTemplate redis) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.redis = redis;
    }

    public SeatMapResponse getSeatMap(Long eventId) {
        // A cheap primary-key lookup, so an unknown event is a clear 404 rather than an empty map
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }

        // 1 SQL query: every seat, already sorted A-1, A-2 ... with booked = true/false
        List<SeatRepository.SeatMapRow> seats = seatRepository.findSeatMap(eventId);
        if (seats.isEmpty()) {
            return new SeatMapResponse(eventId, List.of());
        }

        // 1 Redis call: MGET answers in the same order as the keys, null = not held
        List<Long> seatIds = seats.stream().map(SeatRepository.SeatMapRow::getId).toList();
        List<String> holders = redis.opsForValue().multiGet(HoldKeys.of(eventId, seatIds));

        // Group into rows, keeping the query's order (LinkedHashMap remembers insertion order)
        Map<String, List<SeatResponse>> rows = new LinkedHashMap<>();
        for (int i = 0; i < seats.size(); i++) {
            SeatRepository.SeatMapRow seat = seats.get(i);
            String holder = holders == null ? null : holders.get(i);
            rows.computeIfAbsent(seat.getRowLabel(), row -> new ArrayList<>())
                    .add(new SeatResponse(seat.getId(), seat.getNumber(), statusOf(seat.isBooked(), holder)));
        }

        List<RowResponse> rowResponses = rows.entrySet().stream()
                .map(entry -> new RowResponse(entry.getKey(), entry.getValue()))
                .toList();
        return new SeatMapResponse(eventId, rowResponses);
    }

    // BOOKED wins over HELD: a sold seat stays sold even if an old hold key hasn't expired yet
    private static SeatStatus statusOf(boolean booked, String holder) {
        if (booked) {
            return SeatStatus.BOOKED;
        }
        return holder != null ? SeatStatus.HELD : SeatStatus.AVAILABLE;
    }
}
