package com.Hao.ticketbooking.booking;

// The (mock) payment was declined → 402. Thrown inside the booking transaction, so it rolls back.
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException() {
        super("Payment declined");
    }
}
