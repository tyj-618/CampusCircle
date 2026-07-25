package com.tyj.campuscircle.post;

import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisHotPostRankStore.class);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

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
        Set<String> postIdValues;
        try {
            postIdValues = stringRedisTemplate.opsForZSet().reverseRange(key, 0, limit - 1L);
            if ((postIdValues == null || postIdValues.isEmpty())
                    && Boolean.TRUE.equals(stringRedisTemplate.hasKey(emptyKey(key)))) {
                return List.of();
            }
        } catch (DataAccessException ex) {
            LOGGER.warn("Redis hot-post cache unavailable, falling back to database", ex);
            return dbLoader.get();
        }

        if (postIdValues == null || postIdValues.isEmpty()) {
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
        executeCacheUpdate("increase hot-post score", () -> {
            incrementIfPresent(ALL_POSTS_KEY, postId, delta);
            if (categoryId != null) {
                incrementIfPresent(buildCategoryKey(categoryId), postId, delta);
            }
        });
    }

    @Override
    public void decreaseScore(Long postId, Long categoryId, double delta) {
        executeCacheUpdate("decrease hot-post score", () -> {
            incrementIfPresent(ALL_POSTS_KEY, postId, -delta);
            if (categoryId != null) {
                incrementIfPresent(buildCategoryKey(categoryId), postId, -delta);
            }
        });
    }

    @Override
    public void removePost(Long postId, Long categoryId) {
        executeCacheUpdate("remove post from hot-post cache", () -> {
            removeIfPresent(ALL_POSTS_KEY, postId);
            if (categoryId != null) {
                removeIfPresent(buildCategoryKey(categoryId), postId);
            }
        });
    }

    @Override
    public void moveCategory(Long postId, Long oldCategoryId, Long newCategoryId, double hotScore) {
        executeCacheUpdate("move post between hot-post categories", () -> {
            if (oldCategoryId != null) {
                removeIfPresent(buildCategoryKey(oldCategoryId), postId);
            }
            if (newCategoryId != null) {
                addIfPresent(buildCategoryKey(newCategoryId), postId, hotScore);
            }
        });
    }

    private List<PostHotItemResponse> reloadFromDatabase(String key, Supplier<List<PostHotItemResponse>> dbLoader) {
        String lockToken = UUID.randomUUID().toString();
        Boolean locked;
        try {
            locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey(key), lockToken, rebuildLockTtl);
        } catch (DataAccessException ex) {
            LOGGER.warn("Redis rebuild lock unavailable, falling back to database", ex);
            return dbLoader.get();
        }
        if (!Boolean.TRUE.equals(locked)) {
            return dbLoader.get();
        }

        try {
            List<PostHotItemResponse> hotPosts = dbLoader.get();
            try {
                stringRedisTemplate.delete(key);
                stringRedisTemplate.delete(emptyKey(key));
                if (hotPosts.isEmpty()) {
                    stringRedisTemplate.opsForValue().set(emptyKey(key), "1", emptyCacheTtl);
                } else {
                    for (PostHotItemResponse hotPost : hotPosts) {
                        stringRedisTemplate.opsForZSet().add(key, hotPost.id().toString(), hotPost.hotScore());
                    }
                    refreshRankTtl(key);
                }
            } catch (DataAccessException ex) {
                LOGGER.warn("Failed to rebuild Redis hot-post cache; returning database result", ex);
            }
            return hotPosts;
        } finally {
            releaseLock(key, lockToken);
        }
    }

    private void executeCacheUpdate(String operation, Runnable cacheUpdate) {
        try {
            cacheUpdate.run();
        } catch (DataAccessException ex) {
            LOGGER.warn("Redis unavailable while attempting to {}; primary business result is preserved", operation, ex);
        }
    }

    private void releaseLock(String key, String lockToken) {
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey(key)), lockToken);
        } catch (DataAccessException ex) {
            LOGGER.warn("Failed to release Redis rebuild lock; it will expire automatically", ex);
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
