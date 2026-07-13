package com.tyj.campuscircle.post;

import java.time.LocalDateTime;

public record PostListItemResponse(
        Long id,
        String title,
        String summary,
        PostSchoolResponse school,
        PostCategoryResponse category,
        PostAuthorResponse author,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Double hotScore,
        LocalDateTime createdAt
) {

    public static PostListItemResponse from(PostListItem item) {
        return new PostListItemResponse(
                item.id(),
                item.title(),
                buildSummary(item.content()),
                new PostSchoolResponse(item.schoolId(), item.schoolName(), item.schoolCity()),
                new PostCategoryResponse(item.categoryId(), item.categoryName(), item.categoryCode()),
                new PostAuthorResponse(item.authorId(), item.authorNickname(), item.authorAvatarUrl()),
                item.viewCount(),
                item.likeCount(),
                item.commentCount(),
                item.hotScore(),
                item.createdAt()
        );
    }

    private static String buildSummary(String content) {
        if (content == null || content.length() <= 80) {
            return content;
        }
        return content.substring(0, 80) + "...";
    }
}
