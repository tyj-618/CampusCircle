package com.tyj.campuscircle.event;

public record CommentCreatedEvent(
        Long receiverId,
        Long senderId,
        Long postId,
        Long commentId
) {
}
