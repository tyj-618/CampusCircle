package com.tyj.campuscircle.ai;

import com.tyj.campuscircle.post.PostListItem;

import java.time.LocalDateTime;

public record RetrievedPost(
        Long id,
        String title,
        String content,
        String schoolName,
        LocalDateTime createdAt
) {
    static RetrievedPost from(PostListItem post) {
        return new RetrievedPost(post.id(), post.title(), post.content(), post.schoolName(), post.createdAt());
    }

    String excerpt() {
        String normalized = content() == null ? "" : content().trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160) + "...";
    }
}
