package com.tyj.campuscircle.post;

import java.util.List;
import java.util.function.Supplier;

public interface HotPostRankStore {

    double VIEW_SCORE = 1.0;
    double LIKE_SCORE = 2.0;
    double COMMENT_SCORE = 3.0;

    List<PostHotItemResponse> listHotPosts(int limit, Long categoryId, Supplier<List<PostHotItemResponse>> dbLoader);

    default void increaseScore(Long postId, Long categoryId, double delta) {
    }

    default void decreaseScore(Long postId, Long categoryId, double delta) {
    }

    default void removePost(Long postId, Long categoryId) {
    }

    default void moveCategory(Long postId, Long oldCategoryId, Long newCategoryId, double hotScore) {
    }
}
