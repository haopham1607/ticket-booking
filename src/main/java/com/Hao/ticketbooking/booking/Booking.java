package com.Hao.ticketbooking.booking;

import com.Hao.ticketbooking.event.Event;
import com.Hao.ticketbooking.event.Seat;
import com.Hao.ticketbooking.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// One purchase by one user for one event. Cancelling changes the status; rows are never deleted.
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // A booking owns its seats (at most 6): saving the booking also saves them (cascade)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingSeat> seats = new ArrayList<>();

    protected Booking() {
        // for JPA only
    }

    public Booking(User user, Event event) {
        this.user = user;
        this.event = event;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = Instant.now();
    }

    public void addSeat(Seat seat) {
        seats.add(new BookingSeat(this, seat));
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Event getEvent() {
        return event;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<BookingSeat> getSeats() {
        return seats;
    }
}
