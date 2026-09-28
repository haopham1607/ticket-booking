package com.Hao.ticketbooking.hold;

import com.Hao.ticketbooking.hold.dto.HoldRequest;
import com.Hao.ticketbooking.hold.dto.HoldResponse;
import com.Hao.ticketbooking.hold.dto.ReleaseRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Requires a logged-in user: /api/holds isn't public, so anyRequest().authenticated() applies.
// The user id always comes from the verified token, never from the request body.
@RestController
@RequestMapping("/api/holds")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    public HoldResponse hold(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HoldRequest request) {
        return holdService.hold(Long.valueOf(jwt.getSubject()), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> release(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReleaseRequest request) {
        holdService.release(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }
}
