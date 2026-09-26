package com.Hao.ticketbooking.auth;

// Same message whether the email or the password was wrong,
// so nobody can find out which emails are registered.
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
