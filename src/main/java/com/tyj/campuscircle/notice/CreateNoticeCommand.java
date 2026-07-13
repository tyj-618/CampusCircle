package com.tyj.campuscircle.notice;

public record CreateNoticeCommand(
        Long receiverId,
        Long senderId,
        Long postId,
        Long commentId,
        Integer type,
        String eventKey,
        String content
) {
}
