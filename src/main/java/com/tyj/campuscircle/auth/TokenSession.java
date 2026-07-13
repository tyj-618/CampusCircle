package com.tyj.campuscircle.auth;

public record TokenSession(
        String token,
        Long userId,
        long expiresIn
) {
}
