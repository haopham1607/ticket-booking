package com.Hao.ticketbooking.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // All seats for an event in ONE statement: every row (A, B, ...) combined with
    // every seat number (1, 2, ...). chr(65) is 'A', so row 1 → 'A'. Returns rows inserted.
    @Modifying
    @Query(value = """
            INSERT INTO seats (event_id, row_label, number)
            SELECT :eventId, chr(64 + r), n
            FROM generate_series(1, :rows) AS r,
                 generate_series(1, :seatsPerRow) AS n
            """, nativeQuery = true)
    int generateSeats(@Param("eventId") Long eventId,
                      @Param("rows") int rows,
                      @Param("seatsPerRow") int seatsPerRow);

    long countByEventId(Long eventId);

    // Seat counts for many events in one query, instead of one count per event (N+1)
    @Query("""
            SELECT s.event.id AS eventId, COUNT(s) AS seatCount
            FROM Seat s
            WHERE s.event.id IN :eventIds
            GROUP BY s.event.id
            """)
    List<EventSeatCount> countByEventIds(@Param("eventIds") List<Long> eventIds);

    // One result row of countByEventIds; Spring fills it from the query's aliases
    interface EventSeatCount {
        Long getEventId();

        long getSeatCount();
    }
}
