package com.tyj.campuscircle.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Profile("redis")
public class RedisTokenStore implements TokenStore {

    private static final Duration TOKEN_TTL = Duration.ofHours(2);
    private static final String TOKEN_KEY_PREFIX = "campuscircle:auth:token:";

    private final TokenGenerator tokenGenerator;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenStore(TokenGenerator tokenGenerator, StringRedisTemplate stringRedisTemplate) {
        this.tokenGenerator = tokenGenerator;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public TokenSession createSession(Long userId) {
        String token = tokenGenerator.generate();
        stringRedisTemplate.opsForValue().set(buildTokenKey(token), userId.toString(), TOKEN_TTL);
        return new TokenSession(token, userId, TOKEN_TTL.toSeconds());
    }

    @Override
    public Optional<Long> findUserId(String token) {
        String userId = stringRedisTemplate.opsForValue().get(buildTokenKey(token));
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(userId));
    }

    @Override
    public void remove(String token) {
        stringRedisTemplate.delete(buildTokenKey(token));
    }

    private String buildTokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }
}
