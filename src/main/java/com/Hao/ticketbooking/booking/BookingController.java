package com.Hao.ticketbooking.booking;

import com.Hao.ticketbooking.booking.dto.BookingRequest;
import com.Hao.ticketbooking.booking.dto.BookingResponse;
import com.Hao.ticketbooking.booking.dto.MyBookingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Requires a logged-in user. The user id always comes from the verified token.
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> confirm(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody BookingRequest request) {
        BookingResponse booking = bookingService.confirm(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/me")
    public List<MyBookingResponse> myBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.listMine(Long.valueOf(jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        bookingService.cancel(Long.valueOf(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}
