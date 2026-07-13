package com.tyj.campuscircle.post;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        PostSchoolResponse school,
        PostCategoryResponse category,
        PostAuthorResponse author,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Boolean liked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PostDetailResponse from(PostDetail postDetail, boolean liked) {
        return new PostDetailResponse(
                postDetail.id(),
                postDetail.title(),
                postDetail.content(),
                new PostSchoolResponse(postDetail.schoolId(), postDetail.schoolName(), postDetail.schoolCity()),
                new PostCategoryResponse(postDetail.categoryId(), postDetail.categoryName(), postDetail.categoryCode()),
                new PostAuthorResponse(postDetail.userId(), postDetail.authorNickname(), postDetail.authorAvatarUrl()),
                postDetail.viewCount(),
                postDetail.likeCount(),
                postDetail.commentCount(),
                liked,
                postDetail.createdAt(),
                postDetail.updatedAt()
        );
    }
}
