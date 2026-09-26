package com.Hao.ticketbooking.user.me;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    // Everything here comes from the verified token; there is no database lookup
    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        Long id = Long.valueOf(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new MeResponse(id, role, authorities);
    }
}
