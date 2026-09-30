package com.Hao.ticketbooking.event;

import com.Hao.ticketbooking.event.dto.EventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache-aside storage for event details in Redis: key event:{id}, value = EventResponse as JSON.
 * Event details practically never change after creation, so caching them is safe.
 * (The seat map is deliberately NOT cached: holds change every few seconds.)
 *
 * The cache is only an optimization: if Redis fails, it logs and behaves like a cache miss,
 * so the event is simply read from Postgres instead of the request failing.
 */
@Component
public class EventCache {

    private static final Logger log = LoggerFactory.getLogger(EventCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Duration ttl;

    public EventCache(StringRedisTemplate redis,
                      ObjectMapper json,
                      @Value("${events.cache-ttl-seconds}") long ttlSeconds) {
        this.redis = redis;
        this.json = json;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<EventResponse> get(Long eventId) {
        try {
            String cached = redis.opsForValue().get(key(eventId));
            return cached == null ? Optional.empty() : Optional.of(json.readValue(cached, EventResponse.class));
        } catch (RuntimeException e) {
            log.warn("Event cache read failed for event {}; falling back to the database", eventId, e);
            return Optional.empty();
        }
    }

    public void put(EventResponse event) {
        try {
            redis.opsForValue().set(key(event.id()), json.writeValueAsString(event), ttl);
        } catch (RuntimeException e) {
            log.warn("Event cache write failed for event {}", event.id(), e);
        }
    }

    // Call this from any future endpoint that changes or deletes an event
    public void evict(Long eventId) {
        try {
            redis.delete(key(eventId));
        } catch (RuntimeException e) {
            log.warn("Event cache evict failed for event {}", eventId, e);
        }
    }

    private static String key(Long eventId) {
        return "event:" + eventId;
    }
}
