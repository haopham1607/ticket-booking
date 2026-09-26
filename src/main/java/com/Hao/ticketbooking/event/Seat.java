package com.Hao.ticketbooking.event;

import jakarta.persistence.*;

// Seats are created in bulk with one SQL statement (see SeatRepository),
// so there is no public constructor: Java only ever reads them.
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many seats belong to one event. LAZY: the event is only loaded if it's actually used.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(nullable = false)
    private int number;

    protected Seat() {
        // for JPA only
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getNumber() {
        return number;
    }
}
