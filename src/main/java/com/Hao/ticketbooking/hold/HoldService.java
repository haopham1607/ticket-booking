package com.Hao.ticketbooking.hold;

import com.Hao.ticketbooking.common.RateLimiter;
import com.Hao.ticketbooking.event.SeatRepository;
import com.Hao.ticketbooking.hold.dto.HoldRequest;
import com.Hao.ticketbooking.hold.dto.HoldResponse;
import com.Hao.ticketbooking.hold.dto.ReleaseRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/**
 * Holds seats in Redis while a user checks out. A hold is temporary: it expires on its own,
 * and it is not a booking. Postgres (the partial unique index) remains the final guarantee.
 */
@Service
public class HoldService {

    private final SeatRepository seatRepository;
    private final StringRedisTemplate redis;
    private final RedisScript<Long> holdSeatsScript;
    private final RedisScript<Long> releaseSeatsScript;
    private final RateLimiter rateLimiter;
    private final Duration holdTtl;
    private final int maxAttempts;
    private final Duration rateWindow;

    public HoldService(SeatRepository seatRepository,
                       StringRedisTemplate redis,
                       RedisScript<Long> holdSeatsScript,
                       RedisScript<Long> releaseSeatsScript,
                       RateLimiter rateLimiter,
                       @Value("${holds.ttl-seconds}") long holdTtlSeconds,
                       @Value("${holds.rate-limit.max-attempts}") int maxAttempts,
                       @Value("${holds.rate-limit.window-seconds}") long rateWindowSeconds) {
        this.seatRepository = seatRepository;
        this.redis = redis;
        this.holdSeatsScript = holdSeatsScript;
        this.releaseSeatsScript = releaseSeatsScript;
        this.rateLimiter = rateLimiter;
        this.holdTtl = Duration.ofSeconds(holdTtlSeconds);
        this.maxAttempts = maxAttempts;
        this.rateWindow = Duration.ofSeconds(rateWindowSeconds);
    }

    /**
     * Holds all requested seats for this user, or none of them.
     * Checks run cheapest first, and only the last step (the Lua script) changes anything.
     */
    public HoldResponse hold(Long userId, HoldRequest request) {
        Long eventId = request.eventId();
        List<Long> seatIds = request.seatIds();

        // 1. Every attempt counts, successful or not → 429
        rateLimiter.check("rate:hold:" + userId, maxAttempts, rateWindow);

        // 2. Plain Java: a Set drops duplicates, so a smaller Set means a repeated id → 400
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new InvalidSeatsException("Duplicate seat ids");
        }

        // 3. One query covers unknown seats, seats of another event, and an unknown event → 400
        if (seatRepository.countByEventIdAndIdIn(eventId, seatIds) != seatIds.size()) {
            throw new InvalidSeatsException("Some seats don't exist or don't belong to this event");
        }

        // 4. Already sold (Postgres) → 409, naming exactly which seats
        List<Long> booked = seatRepository.findBookedSeatIds(seatIds);
        if (!booked.isEmpty()) {
            throw new SeatsUnavailableException(booked);
        }

        // 5. Hold them all atomically. 0 means another user holds at least one of them → 409.
        //    The script doesn't say which, so every requested seat is listed.
        Long held = redis.execute(holdSeatsScript, HoldKeys.of(eventId, seatIds),
                userId.toString(), String.valueOf(holdTtl.toSeconds()));
        if (held == null || held == 0) {
            throw new SeatsUnavailableException(seatIds);
        }

        return new HoldResponse(eventId, seatIds, Instant.now().plus(holdTtl));
    }

    /**
     * Releases only the seats this user holds. Seats held by others (or not held) are skipped
     * silently, so the response never reveals anything about other users' holds.
     */
    public void release(Long userId, ReleaseRequest request) {
        redis.execute(releaseSeatsScript, HoldKeys.of(request.eventId(), request.seatIds()), userId.toString());
    }
}
