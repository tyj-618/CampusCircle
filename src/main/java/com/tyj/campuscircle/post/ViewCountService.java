package com.tyj.campuscircle.post;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class ViewCountService implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewCountService.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ViewCountMapper viewCountMapper;
    private final HotPostRankStore hotPostRankStore;
    private final long flushIntervalSeconds;
    private final ReentrantReadWriteLock pendingViewLock = new ReentrantReadWriteLock();
    private final ScheduledThreadPoolExecutor scheduler;
    private Map<Long, LongAdder> pendingViewCounts = new ConcurrentHashMap<>();

    public ViewCountService(
            ViewCountMapper viewCountMapper,
            HotPostRankStore hotPostRankStore,
            @Value("${campuscircle.view-count.flush-interval-seconds:10}") long flushIntervalSeconds) {
        this.viewCountMapper = viewCountMapper;
        this.hotPostRankStore = hotPostRankStore;
        this.flushIntervalSeconds = flushIntervalSeconds;
        this.scheduler = new ScheduledThreadPoolExecutor(
                1,
                new ViewCountThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.scheduler.setRemoveOnCancelPolicy(true);
    }

    public void recordView(Long postId, Long categoryId) {
        Lock readLock = pendingViewLock.readLock();
        readLock.lock();
        try {
            pendingViewCounts.computeIfAbsent(postId, key -> new LongAdder()).increment();
        } finally {
            readLock.unlock();
        }
        hotPostRankStore.increaseScore(postId, categoryId, HotPostRankStore.VIEW_SCORE);
    }

    public void flushPendingViews() {
        Map<Long, LongAdder> batch = drainPendingViewCounts();
        RuntimeException firstFailure = null;
        for (Map.Entry<Long, LongAdder> entry : batch.entrySet()) {
            long delta = entry.getValue().sum();
            if (delta <= 0) {
                continue;
            }
            try {
                viewCountMapper.increaseViewCount(entry.getKey(), delta);
            } catch (RuntimeException exception) {
                requeue(entry.getKey(), delta);
                LOGGER.warn("Failed to flush view count; delta was requeued: postId={}, delta={}",
                        entry.getKey(), delta, exception);
                if (firstFailure == null) {
                    firstFailure = exception;
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    @Override
    public void afterPropertiesSet() {
        scheduler.scheduleWithFixedDelay(
                this::flushSafely,
                flushIntervalSeconds,
                flushIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        flushSafely();
    }

    private void flushSafely() {
        try {
            flushPendingViews();
        } catch (Exception exception) {
            LOGGER.warn("View-count flush failed; pending deltas will be retried", exception);
        }
    }

    private Map<Long, LongAdder> drainPendingViewCounts() {
        Lock writeLock = pendingViewLock.writeLock();
        writeLock.lock();
        try {
            Map<Long, LongAdder> batch = pendingViewCounts;
            pendingViewCounts = new ConcurrentHashMap<>();
            return batch;
        } finally {
            writeLock.unlock();
        }
    }

    private void requeue(Long postId, long delta) {
        Lock readLock = pendingViewLock.readLock();
        readLock.lock();
        try {
            pendingViewCounts.computeIfAbsent(postId, key -> new LongAdder()).add(delta);
        } finally {
            readLock.unlock();
        }
    }

    private static class ViewCountThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "campuscircle-view-count-flusher");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignored, exception) ->
                    LOGGER.error("View-count scheduler terminated unexpectedly", exception));
            return thread;
        }
    }
}
