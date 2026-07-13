package com.tyj.campuscircle.notice;

import java.time.LocalDateTime;

public record NoticeItem(
        Long id,
        Long receiverId,
        Long senderId,
        String senderNickname,
        String senderAvatarUrl,
        Long postId,
        Long commentId,
        Integer type,
        String content,
        Integer readStatus,
        LocalDateTime createdAt
) {
}
