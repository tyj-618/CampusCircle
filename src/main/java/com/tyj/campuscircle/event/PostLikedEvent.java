package com.tyj.campuscircle.event;

import java.time.Instant;
import java.util.UUID;

public record PostLikedEvent(
        String eventId,
        Instant occurredAt,
        Long receiverId,
        Long senderId,
        Long postId
) {

    public static PostLikedEvent create(Long receiverId, Long senderId, Long postId) {
        return new PostLikedEvent(UUID.randomUUID().toString(), Instant.now(), receiverId, senderId, postId);
    }
}
