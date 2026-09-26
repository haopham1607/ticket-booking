package com.Hao.ticketbooking.config;

import com.Hao.ticketbooking.user.Role;
import com.Hao.ticketbooking.user.User;
import com.Hao.ticketbooking.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the admin account on startup from ADMIN_EMAIL / ADMIN_PASSWORD.
 * Registration always creates USER, so this is the only way an ADMIN can exist.
 * Safe to run on every startup: it does nothing if the account already exists.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${admin.email}") String email,
                       @Value("${admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set; no admin account created");
            return;
        }
        if (password.length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters; no admin account created");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }

        try {
            userRepository.saveAndFlush(new User(email, passwordEncoder.encode(password), Role.ADMIN));
            log.info("Created admin account {}", email);   // never log the password
        } catch (DataIntegrityViolationException e) {
            // Another instance starting at the same moment created it first (Phase 8)
            log.info("Admin account {} already exists", email);
        }
    }
}
