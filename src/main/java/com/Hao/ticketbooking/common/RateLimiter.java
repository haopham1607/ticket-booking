package com.Hao.ticketbooking.common;

import com.Hao.ticketbooking.hold.RateLimitExceededException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fixed-window rate limiter in Redis. The window starts at a key's first attempt and
 * lasts windowLength; the counter then expires and the next attempt starts a new window.
 * Kept in Redis (not in memory) so every app instance shares the same counts.
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redis;
    private final RedisScript<Long> rateLimitScript;

    public RateLimiter(StringRedisTemplate redis, RedisScript<Long> rateLimitScript) {
        this.redis = redis;
        this.rateLimitScript = rateLimitScript;
    }

    /**
     * Counts one attempt for this key and throws if it goes over the limit.
     *
     * @param key          what is being limited, e.g. "rate:hold:1" (hold attempts by user 1)
     * @param maxAttempts  attempts allowed per window
     * @param windowLength how long a window lasts
     */
    public void check(String key, int maxAttempts, Duration windowLength) {
        Long count = redis.execute(rateLimitScript, List.of(key), String.valueOf(windowLength.toSeconds()));
        if (count != null && count > maxAttempts) {
            // Retry-After is simply the time left before the counter expires
            Long secondsLeft = redis.getExpire(key, TimeUnit.SECONDS);
            long retryAfter = (secondsLeft != null && secondsLeft > 0) ? secondsLeft : windowLength.toSeconds();
            throw new RateLimitExceededException(retryAfter);
        }
    }
}
