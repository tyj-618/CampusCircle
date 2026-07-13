package com.tyj.campuscircle.event;

public record PostLikedEvent(
        Long receiverId,
        Long senderId,
        Long postId
) {
}
