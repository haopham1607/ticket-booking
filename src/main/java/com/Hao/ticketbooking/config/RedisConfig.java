package com.Hao.ticketbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Loads the Lua scripts from src/main/resources/scripts. Each one runs inside Redis
 * as a single atomic step. Lua numbers come back to Java as Long.
 */
@Configuration
public class RedisConfig {

    // Returns 1 if every seat is now held by the user, 0 if any is held by someone else
    @Bean
    RedisScript<Long> holdSeatsScript() {
        return RedisScript.of(new ClassPathResource("scripts/hold_seats.lua"), Long.class);
    }

    // Returns how many of the user's own holds were released
    @Bean
    RedisScript<Long> releaseSeatsScript() {
        return RedisScript.of(new ClassPathResource("scripts/release_seats.lua"), Long.class);
    }

    // Returns the attempt count in the current rate-limit window
    @Bean
    RedisScript<Long> rateLimitScript() {
        return RedisScript.of(new ClassPathResource("scripts/rate_limit.lua"), Long.class);
    }
}
