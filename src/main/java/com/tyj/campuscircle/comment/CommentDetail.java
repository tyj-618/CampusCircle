package com.tyj.campuscircle.comment;

import java.time.LocalDateTime;

public record CommentDetail(
        Long id,
        Long postId,
        Long userId,
        Long postAuthorId,
        String content,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
