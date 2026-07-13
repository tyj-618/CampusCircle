package com.tyj.campuscircle.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!redis")
public class InMemoryTokenStore implements TokenStore {

    private static final Duration TOKEN_TTL = Duration.ofHours(2);

    private final TokenGenerator tokenGenerator;
    private final Map<String, StoredToken> tokens = new ConcurrentHashMap<>();

    public InMemoryTokenStore(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public TokenSession createSession(Long userId) {
        String token = tokenGenerator.generate();
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);
        tokens.put(token, new StoredToken(userId, expiresAt));
        return new TokenSession(token, userId, TOKEN_TTL.toSeconds());
    }

    @Override
    public Optional<Long> findUserId(String token) {
        StoredToken storedToken = tokens.get(token);
        if (storedToken == null) {
            return Optional.empty();
        }

        if (storedToken.expiresAt().isBefore(Instant.now())) {
            tokens.remove(token);
            return Optional.empty();
        }

        return Optional.of(storedToken.userId());
    }

    @Override
    public void remove(String token) {
        tokens.remove(token);
    }

    private record StoredToken(Long userId, Instant expiresAt) {
    }
}
