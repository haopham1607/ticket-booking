package com.Hao.ticketbooking.user.me;

import java.util.List;

public record MeResponse(Long id, String role, List<String> authorities) {
}
