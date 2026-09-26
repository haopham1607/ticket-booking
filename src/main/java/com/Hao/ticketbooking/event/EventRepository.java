package com.Hao.ticketbooking.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // WHERE starts_at > ? ORDER BY starts_at ASC
    List<Event> findByStartsAtAfterOrderByStartsAtAsc(Instant now);
}
