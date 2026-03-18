package com.github.xjesusx0.cloudbox.application.services;

import com.github.xjesusx0.cloudbox.application.dtos.AuthRequest;
import com.github.xjesusx0.cloudbox.application.dtos.AuthResponse;
import com.github.xjesusx0.cloudbox.core.exceptions.AuthenticationException;
import com.github.xjesusx0.cloudbox.core.exceptions.UserAlreadyExistsException;
import com.github.xjesusx0.cloudbox.domain.models.User;
import com.github.xjesusx0.cloudbox.domain.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException(request.username());
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);
        return new AuthResponse(token);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("User account is disabled");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }
}
