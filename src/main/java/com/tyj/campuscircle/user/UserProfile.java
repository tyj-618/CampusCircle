package com.tyj.campuscircle.user;

import java.time.LocalDateTime;

public record UserProfile(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        Integer role,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
