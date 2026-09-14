package com.tutoring.dto.response;

import com.tutoring.entity.User;

public record RegisterResponse(Long id, String email, User.Role role) {
}
