package com.ejr.managekitchen.operations.user.dto;

import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.operations.user.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, String fullName, Role role, boolean enabled,
                           LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isEnabled(), user.getCreatedAt());
    }
}
