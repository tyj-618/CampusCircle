package com.tyj.campuscircle.ai;

public record AiPostReference(
        Long postId,
        String title,
        String schoolName,
        String excerpt
) {
    static AiPostReference from(RetrievedPost post) {
        return new AiPostReference(post.id(), post.title(), post.schoolName(), post.excerpt());
    }
}
