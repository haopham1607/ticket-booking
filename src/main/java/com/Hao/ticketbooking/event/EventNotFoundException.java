package com.Hao.ticketbooking.event;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long id) {
        super("Event not found: " + id);
    }
}
