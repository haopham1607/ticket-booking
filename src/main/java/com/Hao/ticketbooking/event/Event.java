package com.Hao.ticketbooking.event;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String venue;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    // Money as integer cents: $49.99 is stored as 4999
    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
        // for JPA only
    }

    public Event(String name, String venue, Instant startsAt, int priceCents) {
        this.name = name;
        this.venue = venue;
        this.startsAt = startsAt;
        this.priceCents = priceCents;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVenue() {
        return venue;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
