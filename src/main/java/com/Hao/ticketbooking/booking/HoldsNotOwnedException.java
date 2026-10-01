package com.Hao.ticketbooking.booking;

// The user tried to confirm seats they don't hold (never held, expired, or held by someone else) → 409
public class HoldsNotOwnedException extends RuntimeException {

    public HoldsNotOwnedException() {
        super("User does not hold all requested seats");
    }
}
