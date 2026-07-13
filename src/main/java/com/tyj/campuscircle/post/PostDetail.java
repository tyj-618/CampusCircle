package com.tyj.campuscircle.post;

import java.time.LocalDateTime;

public record PostDetail(
        Long id,
        Long userId,
        Long schoolId,
        Long categoryId,
        String title,
        String content,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String schoolName,
        String schoolCity,
        String categoryName,
        String categoryCode,
        String authorNickname,
        String authorAvatarUrl,
        Integer authorRole,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Double hotScore
) {
}
