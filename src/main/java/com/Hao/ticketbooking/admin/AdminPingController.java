package com.Hao.ticketbooking.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Temporary: proves role checks work. Delete once real admin endpoints exist (Phase 2).
@RestController
public class AdminPingController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/ping")
    public Map<String, String> ping() {
        return Map.of("message", "hello admin");
    }
}
