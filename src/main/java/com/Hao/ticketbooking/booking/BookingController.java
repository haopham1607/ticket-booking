package com.Hao.ticketbooking.booking;

import com.Hao.ticketbooking.booking.dto.BookingRequest;
import com.Hao.ticketbooking.booking.dto.BookingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
