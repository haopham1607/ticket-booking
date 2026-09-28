package com.Hao.ticketbooking.hold;

// Duplicate seat ids, or seats that don't exist / belong to another event → 400
public class InvalidSeatsException extends RuntimeException {

    public InvalidSeatsException(String message) {
        super(message);
    }
}
