package com.tyj.campuscircle.like;

public record LikeRecord(
        Long id,
        Long postId,
        Long userId,
        Integer status
) {
}
