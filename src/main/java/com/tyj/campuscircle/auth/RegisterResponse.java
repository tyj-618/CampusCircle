package com.tyj.campuscircle.auth;

public record RegisterResponse(
        Long userId,
        String username,
        String nickname
) {
}
