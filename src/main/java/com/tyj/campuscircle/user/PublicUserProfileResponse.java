package com.tyj.campuscircle.user;

import java.time.LocalDateTime;

public record PublicUserProfileResponse(
        Long id,
        String username,
        String nickname,
        Long schoolId,
        String schoolName,
        String schoolCity,
        String avatarUrl,
        String bio,
        long postCount,
        long commentCount,
        LocalDateTime createdAt
) {
}
