package com.Hao.ticketbooking.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Only @NotBlank: anything else wrong with the credentials should be a 401, not a 400
public record LoginRequest(
        @NotBlank
        String email,

        @NotBlank
        String password
) {
}
