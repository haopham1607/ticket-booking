package com.Hao.ticketbooking.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        // BCrypt only uses the first 72 bytes of a password
        @NotBlank @Size(min = 8, max = 72)
        String password
) {
}
