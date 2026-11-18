package com.ejr.managekitchen.operations.user.auth;

import com.ejr.managekitchen.common.security.JwtService;
import com.ejr.managekitchen.operations.user.auth.dto.AuthResponse;
import com.ejr.managekitchen.operations.user.auth.dto.LoginRequest;
import com.ejr.managekitchen.operations.user.auth.dto.RegisterRequest;
import com.ejr.managekitchen.operations.user.User;
import com.ejr.managekitchen.operations.user.UserRepository;
import com.ejr.managekitchen.operations.user.UserService;
import com.ejr.managekitchen.operations.user.dto.UserResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserService userService,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = userService.registerClient(request.email(), request.password(), request.fullName());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("E-mail ou mot de passe incorrect"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.generate(user.toAuthenticatedUser());
        return new AuthResponse(token, "Bearer", jwtService.expirationSeconds(), UserResponse.from(user));
    }
}
