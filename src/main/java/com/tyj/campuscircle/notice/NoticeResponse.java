package com.tyj.campuscircle.notice;

import com.tyj.campuscircle.post.PostAuthorResponse;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        Integer type,
        String content,
        Integer readStatus,
        PostAuthorResponse sender,
        Long postId,
        Long commentId,
        LocalDateTime createdAt
) {

    public static NoticeResponse from(NoticeItem item) {
        return new NoticeResponse(
                item.id(),
                item.type(),
                item.content(),
                item.readStatus(),
                new PostAuthorResponse(item.senderId(), item.senderNickname(), item.senderAvatarUrl()),
                item.postId(),
                item.commentId(),
                item.createdAt()
        );
    }
}
