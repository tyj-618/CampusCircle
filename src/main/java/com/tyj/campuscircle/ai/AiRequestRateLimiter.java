package com.tyj.campuscircle.ai;

import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiRequestRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final AiProperties properties;
    private final Map<Long, Deque<Long>> requestTimesByUser = new ConcurrentHashMap<>();

    public AiRequestRateLimiter(AiProperties properties) {
        this.properties = properties;
    }

    public void check(Long userId) {
        Deque<Long> requestTimes = requestTimesByUser.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (requestTimes) {
            while (!requestTimes.isEmpty() && now - requestTimes.peekFirst() >= WINDOW_MILLIS) {
                requestTimes.removeFirst();
            }
            if (requestTimes.size() >= properties.getMaxRequestsPerMinute()) {
                throw new BusinessException(ErrorCode.CONFLICT, "智能问答请求过于频繁，请稍后再试");
            }
            requestTimes.addLast(now);
        }
    }
}
