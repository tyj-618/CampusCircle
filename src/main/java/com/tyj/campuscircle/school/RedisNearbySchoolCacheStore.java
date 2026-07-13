package com.tyj.campuscircle.school;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
@Profile("redis")
public class RedisNearbySchoolCacheStore implements NearbySchoolCacheStore {

    private static final String KEY_PREFIX = "campuscircle:school:nearby:";
    private static final TypeReference<List<SchoolResponse>> SCHOOL_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;
    private final int cacheJitterSeconds;

    public RedisNearbySchoolCacheStore(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${campuscircle.nearby-school-cache.ttl-seconds:300}") long cacheTtlSeconds,
            @Value("${campuscircle.nearby-school-cache.jitter-seconds:60}") int cacheJitterSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        this.cacheJitterSeconds = Math.max(cacheJitterSeconds, 0);
    }

    @Override
    public List<SchoolResponse> listNearbySchools(Long schoolId, double radiusKm, Supplier<List<SchoolResponse>> dbLoader) {
        String key = buildKey(schoolId, radiusKm);
        String cachedValue = stringRedisTemplate.opsForValue().get(key);
        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, SCHOOL_LIST_TYPE);
            } catch (JsonProcessingException ex) {
                stringRedisTemplate.delete(key);
            }
        }

        List<SchoolResponse> schools = dbLoader.get();
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(schools), ttlWithJitter());
        } catch (JsonProcessingException ex) {
            return schools;
        }
        return schools;
    }

    private String buildKey(Long schoolId, double radiusKm) {
        return KEY_PREFIX + schoolId + ":radius:" + normalizeRadius(radiusKm);
    }

    private String normalizeRadius(double radiusKm) {
        return BigDecimal.valueOf(radiusKm)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private Duration ttlWithJitter() {
        long jitter = cacheJitterSeconds == 0
                ? 0
                : ThreadLocalRandom.current().nextInt(cacheJitterSeconds + 1);
        return cacheTtl.plusSeconds(jitter);
    }
}
