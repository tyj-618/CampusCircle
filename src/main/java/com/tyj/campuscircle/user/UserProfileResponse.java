package com.tyj.campuscircle.user;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        Long schoolId,
        String schoolName,
        String schoolCity,
        String avatarUrl,
        String bio,
        Integer role,
        Integer status,
        LocalDateTime createdAt
) {

    public static UserProfileResponse from(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.id(),
                userProfile.username(),
                userProfile.nickname(),
                userProfile.schoolId(),
                userProfile.schoolName(),
                userProfile.schoolCity(),
                userProfile.avatarUrl(),
                userProfile.bio(),
                userProfile.role(),
                userProfile.status(),
                userProfile.createdAt()
        );
    }
}
