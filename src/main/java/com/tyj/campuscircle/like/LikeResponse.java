package com.tyj.campuscircle.like;

public record LikeResponse(
        boolean liked,
        int likeCount
) {
}
