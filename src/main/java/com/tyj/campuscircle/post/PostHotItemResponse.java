package com.tyj.campuscircle.post;

public record PostHotItemResponse(
        Long id,
        String title,
        String categoryName,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Double hotScore
) {

    public static PostHotItemResponse from(PostListItem item) {
        return new PostHotItemResponse(
                item.id(),
                item.title(),
                item.categoryName(),
                item.viewCount(),
                item.likeCount(),
                item.commentCount(),
                item.hotScore()
        );
    }
}
