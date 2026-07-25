package com.tyj.campuscircle.school;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisNearbySchoolCacheStoreTests {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisNearbySchoolCacheStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        store = new RedisNearbySchoolCacheStore(redisTemplate, new ObjectMapper(), 300, 0);
    }

    @Test
    void fallsBackToDatabaseWhenRedisReadFails() {
        List<SchoolResponse> expected = List.of(school());
        when(valueOperations.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        List<SchoolResponse> result = store.listNearbySchools(1L, 30, () -> expected);

        assertThat(result).isSameAs(expected);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void returnsDatabaseResultWhenRedisWriteFails() {
        List<SchoolResponse> expected = List.of(school());
        when(valueOperations.get(anyString())).thenReturn(null);
        doThrow(new RedisConnectionFailureException("redis unavailable"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        List<SchoolResponse> result = store.listNearbySchools(1L, 30, () -> expected);

        assertThat(result).isSameAs(expected);
    }

    private SchoolResponse school() {
        return new SchoolResponse(
                1L,
                "南京大学",
                "江苏省",
                "南京市",
                new BigDecimal("32.056"),
                new BigDecimal("118.778"),
                0.0
        );
    }
}
