package com.Hao.ticketbooking.event.dto;

// No holder id on purpose: the seat map only says HELD, never who holds a seat
public record SeatResponse(Long id, int number, SeatStatus status) {
}
