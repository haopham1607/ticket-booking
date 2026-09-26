package com.Hao.ticketbooking.auth.dto;

import com.Hao.ticketbooking.user.Role;
import com.Hao.ticketbooking.user.User;

public record UserResponse(Long id, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
