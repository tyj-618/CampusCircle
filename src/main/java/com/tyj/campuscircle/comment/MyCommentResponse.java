package com.tyj.campuscircle.comment;

import java.time.LocalDateTime;

public record MyCommentResponse(
        Long id,
        Long postId,
        String postTitle,
        String content,
        LocalDateTime createdAt
) {

    public static MyCommentResponse from(MyCommentItem item) {
        return new MyCommentResponse(
                item.id(),
                item.postId(),
                item.postTitle(),
                item.content(),
                item.createdAt()
        );
    }
}
