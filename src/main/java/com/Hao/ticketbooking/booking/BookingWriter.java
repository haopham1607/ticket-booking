package com.Hao.ticketbooking.booking;

import com.Hao.ticketbooking.booking.dto.BookingResponse;
import com.Hao.ticketbooking.event.Event;
import com.Hao.ticketbooking.event.EventNotFoundException;
import com.Hao.ticketbooking.event.EventRepository;
import com.Hao.ticketbooking.event.SeatRepository;
import com.Hao.ticketbooking.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The database part of confirming a booking, as ONE transaction: the booking row, its seat
 * rows and the payment all succeed, or nothing is kept.
 *
 * It is a separate class from BookingService for two reasons:
 * 1. @Transactional only applies when the method is called from another bean.
 * 2. When the unique index rejects a seat, this transaction is broken and must roll back;
 *    the caller catches the exception after that has happened.
 */
@Component
public class BookingWriter {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public BookingWriter(BookingRepository bookingRepository,
                         EventRepository eventRepository,
                         SeatRepository seatRepository,
                         UserRepository userRepository,
                         PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public BookingResponse create(Long userId, Long eventId, List<Long> seatIds) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // getReferenceById gives a placeholder with just the id: no SELECT is needed to link rows
        Booking booking = new Booking(userRepository.getReferenceById(userId), event);
        for (Long seatId : seatIds) {
            booking.addSeat(seatRepository.getReferenceById(seatId));
        }

        // saveAndFlush runs the INSERTs now. If another booking already confirmed one of these
        // seats, the partial unique index rejects it here, before any payment is taken.
        bookingRepository.saveAndFlush(booking);

        // Charge only once the seats are ours. A declined payment throws, which rolls back
        // the inserts above; the user's holds are untouched, so they can retry.
        int totalCents = event.getPriceCents() * seatIds.size();
        paymentService.charge(userId, totalCents);

        return new BookingResponse(booking.getId(), eventId, seatIds, totalCents,
                booking.getStatus(), booking.getCreatedAt());
    }
}
