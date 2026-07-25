package com.tyj.campuscircle.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisHotPostRankStoreTests {

    private static final String RANK_KEY = "campuscircle:rank:post:hot:all";
    private static final String LOCK_KEY = RANK_KEY + ":rebuild-lock";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ZSetOperations<String, String> zSetOperations;
    private RedisHotPostRankStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        zSetOperations = mock(ZSetOperations.class);
        PostMapper postMapper = mock(PostMapper.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        store = new RedisHotPostRankStore(redisTemplate, postMapper, 300, 0, 30, 10);
    }

    @Test
    void releasesRebuildLockWithAtomicLuaScript() {
        when(zSetOperations.reverseRange(RANK_KEY, 0, 9)).thenReturn(Set.of());
        when(redisTemplate.hasKey(RANK_KEY + ":empty")).thenReturn(false);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), eq(Duration.ofSeconds(10))))
                .thenReturn(true);

        assertThat(store.listHotPosts(10, null, List::of)).isEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(List.of(LOCK_KEY)), any(Object[].class));
        assertThat(scriptCaptor.getValue().getScriptAsString())
                .contains("redis.call('get', KEYS[1])")
                .contains("redis.call('del', KEYS[1])");
        verify(valueOperations, never()).get(LOCK_KEY);
        verify(redisTemplate, never()).delete(LOCK_KEY);
    }

    @Test
    void preservesPrimaryBusinessResultWhenHotScoreUpdateFails() {
        when(redisTemplate.hasKey(RANK_KEY))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        assertThatCode(() -> store.increaseScore(1L, null, 1.0))
                .doesNotThrowAnyException();
    }

    @Test
    void fallsBackToDatabaseWhenRedisReadFails() {
        PostHotItemResponse expected = new PostHotItemResponse(1L, "title", "category", 10, 2, 1, 15.0);
        when(zSetOperations.reverseRange(eq(RANK_KEY), anyLong(), anyLong()))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        List<PostHotItemResponse> result = store.listHotPosts(10, null, () -> List.of(expected));

        assertThat(result).containsExactly(expected);
    }
}
