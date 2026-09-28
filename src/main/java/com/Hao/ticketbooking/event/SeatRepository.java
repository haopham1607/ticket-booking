package com.Hao.ticketbooking.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    // How many of these seat ids exist AND belong to this event. If the answer is smaller
    // than the number of ids sent, some seats are unknown or belong to another event.
    long countByEventIdAndIdIn(Long eventId, Collection<Long> seatIds);

    // Which of these seats already have a CONFIRMED booking (permanent, in Postgres).
    // Native SQL: booking_seats has no Java entity yet (that comes with bookings in Phase 5).
    @Query(value = """
            SELECT seat_id FROM booking_seats
            WHERE status = 'CONFIRMED' AND seat_id IN (:seatIds)
            """, nativeQuery = true)
    List<Long> findBookedSeatIds(@Param("seatIds") Collection<Long> seatIds);

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
