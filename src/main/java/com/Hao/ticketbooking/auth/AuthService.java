package com.Hao.ticketbooking.auth;

import com.Hao.ticketbooking.auth.dto.LoginRequest;
import com.Hao.ticketbooking.auth.dto.RegisterRequest;
import com.Hao.ticketbooking.user.Role;
import com.Hao.ticketbooking.user.User;
import com.Hao.ticketbooking.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {
        // 1. One consistent form, so "Alice@Mail.com" and "alice@mail.com" are the same account
        String email = request.email().trim().toLowerCase();

        // 2. Fast check with a clean error for the common case
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        // 3. Never store the raw password
        String passwordHash = passwordEncoder.encode(request.password());

        // 4. Registration always creates a normal user
        User user = new User(email, passwordHash, Role.USER);

        // 5. The UNIQUE constraint catches two sign-ups racing past the check above.
        //    saveAndFlush runs the INSERT now, so the error happens inside this try.
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    public String login(LoginRequest request) {
        // Same normalization as register, so "Alice@Mail.com" finds "alice@mail.com"
        String email = request.email().trim().toLowerCase();

        // Unknown email and wrong password throw the same exception,
        // so the response never reveals which emails are registered
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.generateToken(user);
    }
}
