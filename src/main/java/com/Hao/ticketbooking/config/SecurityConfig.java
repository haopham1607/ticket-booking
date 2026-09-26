package com.Hao.ticketbooking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity   // without this, @PreAuthorize is silently ignored
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // One key, built from jwt.secret. The encoder signs tokens with it now;
    // in Step 6 a decoder will verify tokens with the same key.
    @Bean
    SecretKey jwtSecretKey(@Value("${jwt.secret}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).build();
    }

    // Verifies incoming tokens with the same key: signature, expiry and format.
    // Only HS256 is accepted, whatever the token's own header claims.
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // Turns the token's "role" claim into a Spring authority:
    // "role": "ADMIN"  →  ROLE_ADMIN, which is what hasRole('ADMIN') checks.
    // .jwt(Customizer.withDefaults()) picks this bean up automatically.
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Token-based API: no cookies, so CSRF protection doesn't apply
                .csrf(csrf -> csrf.disable())
                // Never create an HTTP session; every request carries its own token
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Rules are checked top to bottom; the first match wins
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Browsing events is public; creating one still needs a token (and ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Adds the bearer token filter: reads "Authorization: Bearer <token>"
                // on every request and checks it with the JwtDecoder bean.
                // Its entry point handles invalid or expired tokens.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(SecurityConfig::unauthorized)
                )
                // Security rejects requests before any controller runs, so GlobalExceptionHandler
                // never sees these errors. These hooks write the same { error, message } JSON.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(SecurityConfig::unauthorized)   // no token at all
                        .accessDeniedHandler(SecurityConfig::forbidden)           // valid token, wrong role
                );
        return http.build();
    }

    private static void unauthorized(HttpServletRequest request, HttpServletResponse response,
                                     AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    private static void forbidden(HttpServletRequest request, HttpServletResponse response,
                                  AccessDeniedException ex) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    // The messages are fixed text, so building the JSON by hand is safe here
    private static void writeError(HttpServletResponse response, HttpStatus status,
                                   String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
