package com.tyj.campuscircle.auth;

public record LoginResponse(
        String token,
        long expiresIn,
        UserSummary user
) {
}
