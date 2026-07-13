package com.tyj.campuscircle.post;

import java.time.LocalDateTime;

public record PostListItem(
        Long id,
        String title,
        String content,
        Long categoryId,
        String categoryName,
        String categoryCode,
        Long authorId,
        String authorNickname,
        String authorAvatarUrl,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Double hotScore,
        LocalDateTime createdAt
) {
}
