package com.tyj.campuscircle.school;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisNearbySchoolCacheStore.class);
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
        String cachedValue;
        try {
            cachedValue = stringRedisTemplate.opsForValue().get(key);
        } catch (DataAccessException ex) {
            LOGGER.warn("Redis nearby-school cache unavailable, falling back to database", ex);
            return dbLoader.get();
        }

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, SCHOOL_LIST_TYPE);
            } catch (JsonProcessingException ex) {
                evictCorruptedCache(key);
            }
        }

        List<SchoolResponse> schools = dbLoader.get();
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(schools), ttlWithJitter());
        } catch (JsonProcessingException | DataAccessException ex) {
            LOGGER.warn("Failed to write nearby-school cache; returning database result", ex);
        }
        return schools;
    }

    private void evictCorruptedCache(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (DataAccessException ex) {
            LOGGER.warn("Failed to evict corrupted nearby-school cache entry", ex);
        }
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
