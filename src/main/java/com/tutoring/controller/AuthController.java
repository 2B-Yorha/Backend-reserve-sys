package com.tutoring.controller;


import com.tutoring.dto.request.LoginRequest;
import com.tutoring.dto.request.RegisterRequest;
import com.tutoring.dto.response.LoginResponse;
import com.tutoring.dto.response.RegisterResponse;
import com.tutoring.dto.response.UserResponse;
import com.tutoring.entity.User;
import com.tutoring.exception.InvalidCredentialsExcption;
import com.tutoring.repository.UserRepository;
import com.tutoring.security.JwtService;
import com.tutoring.security.RefreshTokenService;
import com.tutoring.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, UserRepository userRepository, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        User user = authService.authenticateAndGetUser(request);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());

        setRefreshCookie(response, refreshToken);

        return ResponseEntity.ok(new LoginResponse(accessToken, jwtService.getExpirationMs()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshCookie(request);
        if (rawRefreshToken != null) {
            refreshTokenService.revoke(rawRefreshToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawRefreshToken = extractRefreshCookie(request);
        if (rawRefreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);

        User user = userRepository.findById(result.userId())
                .orElseThrow(InvalidCredentialsExcption::new);

        String newAccessToken = jwtService.generateToken(user);

        setRefreshCookie(response, result.newRawToken());

        return ResponseEntity.ok(new LoginResponse(newAccessToken, jwtService.getExpirationMs()));
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();
        return ResponseEntity.ok(UserResponse.from(user));
    }


    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(REFRESH_COOKIE_MAX_AGE_SECONDS);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(c.getName())) return c.getValue();
        }
        return null;
    }


}
