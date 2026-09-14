package com.tutoring.dto.response;

import com.tutoring.entity.User;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        User.Role role,
        boolean isActive) {

    public static UserResponse from(User u){
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                u.isActive()
        );
    }
}
