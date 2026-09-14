package com.tutoring.dto.response;

public record LoginResponse(String accessToken, long expiresIn) {
}
