package com.tyj.campuscircle.auth;

public record UserSummary(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        Integer role
) {
}
