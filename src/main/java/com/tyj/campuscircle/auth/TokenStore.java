package com.tyj.campuscircle.auth;

import java.util.Optional;

public interface TokenStore {

    TokenSession createSession(Long userId);

    Optional<Long> findUserId(String token);

    void remove(String token);

    default Optional<String> resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(authorization.substring("Bearer ".length()).trim())
                .filter(token -> !token.isBlank());
    }
}
