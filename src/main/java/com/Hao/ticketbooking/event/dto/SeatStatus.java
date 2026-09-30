package com.Hao.ticketbooking.event.dto;

// Never stored: worked out for each request from Postgres (BOOKED) and Redis (HELD)
public enum SeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
