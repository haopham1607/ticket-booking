package com.Hao.ticketbooking.booking;

import com.Hao.ticketbooking.booking.dto.BookingRequest;
import com.Hao.ticketbooking.booking.dto.BookingResponse;
import com.Hao.ticketbooking.booking.dto.MyBookingResponse;
import com.Hao.ticketbooking.hold.HoldService;
import com.Hao.ticketbooking.hold.InvalidSeatsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/**
 * Confirms a booking: turns the user's Redis holds into permanent rows in Postgres.
 * Two layers protect against double-booking: the hold check here stops almost every
 * conflict, and the partial unique index in Postgres is the final guarantee.
 *
 * confirm() is deliberately not @Transactional: its transaction lives in BookingWriter, so
 * it can react after that has committed or rolled back. listMine() and cancel() are simple
 * single-transaction operations and carry the annotation themselves.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final HoldService holdService;
    private final BookingWriter bookingWriter;
    private final BookingRepository bookingRepository;

    public BookingService(HoldService holdService, BookingWriter bookingWriter, BookingRepository bookingRepository) {
        this.holdService = holdService;
        this.bookingWriter = bookingWriter;
        this.bookingRepository = bookingRepository;
    }

    public BookingResponse confirm(Long userId, BookingRequest request) {
        Long eventId = request.eventId();
        List<Long> seatIds = request.seatIds();

        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new InvalidSeatsException("Duplicate seat ids");
        }

        // Layer 1 (Redis): you can only buy seats you currently hold → 409
        if (!holdService.holdsAll(userId, eventId, seatIds)) {
            throw new HoldsNotOwnedException();
        }

        // Layer 2 (Postgres): a hold can expire in the gap after the check above, letting
        // someone else book the seat first. The unique index then rejects our insert, the
        // transaction rolls back, and we report a conflict rather than a server error → 409.
        BookingResponse booking;
        try {
            booking = bookingWriter.create(userId, eventId, seatIds);
        } catch (DataIntegrityViolationException e) {
            throw new SeatNoLongerAvailableException(e);
        }

        // Only after the commit. Releasing earlier would cost the user both the hold and
        // the booking if the insert failed. If this fails, nothing is lost: the seats are
        // booked in Postgres and the leftover holds expire on their own.
        try {
            holdService.release(userId, eventId, seatIds);
        } catch (RuntimeException e) {
            log.warn("Booking {} committed, but releasing its holds failed; they will expire", booking.bookingId(), e);
        }

        return booking;
    }

    // One query for everything (see the JOIN FETCH), newest booking first
    @Transactional(readOnly = true)
    public List<MyBookingResponse> listMine(Long userId) {
        return bookingRepository.findAllByUserIdWithDetails(userId).stream()
                .map(MyBookingResponse::from)
                .toList();
    }

    /**
     * Cancels the user's own booking. Nothing is deleted: the booking and its seat rows become
     * CANCELLED, the partial unique index stops counting them, and the seats are free again.
     */
    @Transactional
    public void cancel(Long userId, Long bookingId) {
        // Someone else's booking looks exactly like a missing one → 404
        Booking booking = bookingRepository.findByIdAndUserIdWithSeats(bookingId, userId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // Cancelling twice is harmless: the result is the same, so just succeed
        if (booking.isCancelled()) {
            return;
        }

        if (!booking.getEvent().getStartsAt().isAfter(Instant.now())) {
            throw new EventAlreadyStartedException(bookingId);
        }

        // No save() needed: the booking was loaded in this transaction, so JPA notices the
        // changed fields and writes the UPDATEs when the transaction commits (dirty checking)
        booking.cancel();
    }
}
