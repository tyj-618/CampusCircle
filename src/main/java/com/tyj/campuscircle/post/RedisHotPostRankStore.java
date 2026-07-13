package com.tyj.campuscircle.post;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
@Profile("redis")
public class RedisHotPostRankStore implements HotPostRankStore {

    private static final String ALL_POSTS_KEY = "campuscircle:rank:post:hot:all";
    private static final String CATEGORY_KEY_PREFIX = "campuscircle:rank:post:hot:category:";
    private static final String EMPTY_KEY_SUFFIX = ":empty";
    private static final String LOCK_KEY_SUFFIX = ":rebuild-lock";

    private final StringRedisTemplate stringRedisTemplate;
    private final PostMapper postMapper;
    private final Duration rankCacheTtl;
    private final int rankCacheJitterSeconds;
    private final Duration emptyCacheTtl;
    private final Duration rebuildLockTtl;

    public RedisHotPostRankStore(
            StringRedisTemplate stringRedisTemplate,
            PostMapper postMapper,
            @Value("${campuscircle.hot-post-cache.ttl-seconds:300}") long rankCacheTtlSeconds,
            @Value("${campuscircle.hot-post-cache.jitter-seconds:60}") int rankCacheJitterSeconds,
            @Value("${campuscircle.hot-post-cache.empty-ttl-seconds:30}") long emptyCacheTtlSeconds,
            @Value("${campuscircle.hot-post-cache.rebuild-lock-ttl-seconds:10}") long rebuildLockTtlSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.postMapper = postMapper;
        this.rankCacheTtl = Duration.ofSeconds(rankCacheTtlSeconds);
        this.rankCacheJitterSeconds = Math.max(rankCacheJitterSeconds, 0);
        this.emptyCacheTtl = Duration.ofSeconds(emptyCacheTtlSeconds);
        this.rebuildLockTtl = Duration.ofSeconds(rebuildLockTtlSeconds);
    }

    @Override
    public List<PostHotItemResponse> listHotPosts(int limit, Long categoryId, Supplier<List<PostHotItemResponse>> dbLoader) {
        String key = buildKey(categoryId);
        Set<String> postIdValues = stringRedisTemplate.opsForZSet().reverseRange(key, 0, limit - 1L);
        if (postIdValues == null || postIdValues.isEmpty()) {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(emptyKey(key)))) {
                return List.of();
            }
            return reloadFromDatabase(key, dbLoader);
        }

        List<Long> postIds = parsePostIds(postIdValues);
        List<PostHotItemResponse> hotPosts = postMapper.findHotPostsByIds(postIds, categoryId)
                .stream()
                .map(PostHotItemResponse::from)
                .toList();

        if (hotPosts.size() != postIds.size()) {
            return reloadFromDatabase(key, dbLoader);
        }
        refreshRankTtl(key);
        return hotPosts;
    }

    @Override
    public void increaseScore(Long postId, Long categoryId, double delta) {
        incrementIfPresent(ALL_POSTS_KEY, postId, delta);
        if (categoryId != null) {
            incrementIfPresent(buildCategoryKey(categoryId), postId, delta);
        }
    }

    @Override
    public void decreaseScore(Long postId, Long categoryId, double delta) {
        incrementIfPresent(ALL_POSTS_KEY, postId, -delta);
        if (categoryId != null) {
            incrementIfPresent(buildCategoryKey(categoryId), postId, -delta);
        }
    }

    @Override
    public void removePost(Long postId, Long categoryId) {
        removeIfPresent(ALL_POSTS_KEY, postId);
        if (categoryId != null) {
            removeIfPresent(buildCategoryKey(categoryId), postId);
        }
    }

    @Override
    public void moveCategory(Long postId, Long oldCategoryId, Long newCategoryId, double hotScore) {
        if (oldCategoryId != null) {
            removeIfPresent(buildCategoryKey(oldCategoryId), postId);
        }
        if (newCategoryId != null) {
            addIfPresent(buildCategoryKey(newCategoryId), postId, hotScore);
        }
    }

    private List<PostHotItemResponse> reloadFromDatabase(String key, Supplier<List<PostHotItemResponse>> dbLoader) {
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey(key), lockToken, rebuildLockTtl);
        if (!Boolean.TRUE.equals(locked)) {
            return dbLoader.get();
        }

        try {
            List<PostHotItemResponse> hotPosts = dbLoader.get();
            stringRedisTemplate.delete(key);
            stringRedisTemplate.delete(emptyKey(key));
            if (hotPosts.isEmpty()) {
                stringRedisTemplate.opsForValue().set(emptyKey(key), "1", emptyCacheTtl);
                return hotPosts;
            }
            for (PostHotItemResponse hotPost : hotPosts) {
                stringRedisTemplate.opsForZSet().add(key, hotPost.id().toString(), hotPost.hotScore());
            }
            refreshRankTtl(key);
            return hotPosts;
        } finally {
            String currentToken = stringRedisTemplate.opsForValue().get(lockKey(key));
            if (lockToken.equals(currentToken)) {
                stringRedisTemplate.delete(lockKey(key));
            }
        }
    }

    private void incrementIfPresent(String key, Long postId, double delta) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            Double score = stringRedisTemplate.opsForZSet().incrementScore(key, postId.toString(), delta);
            if (score != null && score < 0) {
                stringRedisTemplate.opsForZSet().add(key, postId.toString(), 0);
            }
        }
    }

    private void addIfPresent(String key, Long postId, double score) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForZSet().add(key, postId.toString(), score);
        }
    }

    private void removeIfPresent(String key, Long postId) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForZSet().remove(key, postId.toString());
        }
    }

    private List<Long> parsePostIds(Set<String> postIdValues) {
        List<Long> postIds = new ArrayList<>(postIdValues.size());
        for (String postIdValue : postIdValues) {
            postIds.add(Long.valueOf(postIdValue));
        }
        return postIds;
    }

    private String buildKey(Long categoryId) {
        return categoryId == null ? ALL_POSTS_KEY : buildCategoryKey(categoryId);
    }

    private String buildCategoryKey(Long categoryId) {
        return CATEGORY_KEY_PREFIX + categoryId;
    }

    private String emptyKey(String key) {
        return key + EMPTY_KEY_SUFFIX;
    }

    private String lockKey(String key) {
        return key + LOCK_KEY_SUFFIX;
    }

    private void refreshRankTtl(String key) {
        long jitter = rankCacheJitterSeconds == 0
                ? 0
                : ThreadLocalRandom.current().nextInt(rankCacheJitterSeconds + 1);
        stringRedisTemplate.expire(key, rankCacheTtl.plusSeconds(jitter));
    }
}
