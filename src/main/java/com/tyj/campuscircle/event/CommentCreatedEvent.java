package com.tyj.campuscircle.event;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
        String eventId,
        Instant occurredAt,
        Long receiverId,
        Long senderId,
        Long postId,
        Long commentId
) {

    public static CommentCreatedEvent create(Long receiverId, Long senderId, Long postId, Long commentId) {
        return new CommentCreatedEvent(UUID.randomUUID().toString(), Instant.now(), receiverId, senderId, postId, commentId);
    }
}
