package com.tutoring.service;


import com.tutoring.dto.request.LoginRequest;
import com.tutoring.dto.request.RegisterRequest;
import com.tutoring.dto.response.LoginResponse;
import com.tutoring.dto.response.RegisterResponse;
import com.tutoring.entity.User;
import com.tutoring.exception.DuplicateEmailException;
import com.tutoring.exception.InvalidCredentialsExcption;
import com.tutoring.repository.UserRepository;
import com.tutoring.security.JwtService;
import jakarta.transaction.Transactional;
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

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getRole());
    }

    public User authenticateAndGetUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsExcption::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsExcption();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsExcption();
        }

        return user;
    }


}
