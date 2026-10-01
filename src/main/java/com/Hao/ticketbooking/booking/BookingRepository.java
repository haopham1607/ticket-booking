package com.Hao.ticketbooking.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // A user's bookings with their event, seat rows and seats in ONE query.
    // JOIN FETCH loads the LAZY relationships on purpose, instead of one query per booking (N+1).
    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.event
            LEFT JOIN FETCH b.seats bs
            LEFT JOIN FETCH bs.seat
            WHERE b.user.id = :userId
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findAllByUserIdWithDetails(@Param("userId") Long userId);

    // Finds a booking only if it belongs to this user, so "not found" and "not yours" look the same
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.event
            LEFT JOIN FETCH b.seats
            WHERE b.id = :id AND b.user.id = :userId
            """)
    Optional<Booking> findByIdAndUserIdWithSeats(@Param("id") Long id, @Param("userId") Long userId);
}
