package com.tyj.campuscircle.like;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.PostLikeEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

public interface LikeMapper extends BaseMapper<PostLikeEntity> {

    @Select("SELECT COUNT(*) FROM post WHERE id = #{postId} AND status = 0")
    long countNormalPost(@Param("postId") Long postId);

    default boolean existsNormalPost(Long postId) {
        return countNormalPost(postId) > 0;
    }

    @Select("""
            SELECT id, post_id AS postId, user_id AS userId, status
            FROM post_like
            WHERE post_id = #{postId} AND user_id = #{userId}
            """)
    LikeRecord selectByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    default Optional<LikeRecord> findByPostIdAndUserId(Long postId, Long userId) {
        return Optional.ofNullable(selectByPostIdAndUserId(postId, userId));
    }

    default void saveLike(Long postId, Long userId) {
        PostLikeEntity like = new PostLikeEntity();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setStatus(0);
        insert(like);
    }

    @Update("UPDATE post_like SET status = 0 WHERE id = #{likeId}")
    void activateLike(@Param("likeId") Long likeId);

    @Update("UPDATE post_like SET status = 1 WHERE id = #{likeId}")
    void cancelLike(@Param("likeId") Long likeId);

    @Update("""
            UPDATE post_stat
            SET like_count = like_count + 1,
                hot_score = hot_score + 2
            WHERE post_id = #{postId}
            """)
    void increaseLikeCount(@Param("postId") Long postId);

    @Update("""
            UPDATE post_stat
            SET like_count = GREATEST(like_count - 1, 0),
                hot_score = GREATEST(hot_score - 2, 0)
            WHERE post_id = #{postId}
            """)
    void decreaseLikeCount(@Param("postId") Long postId);

    @Select("SELECT like_count FROM post_stat WHERE post_id = #{postId}")
    Integer selectLikeCount(@Param("postId") Long postId);

    default int findLikeCount(Long postId) {
        Integer count = selectLikeCount(postId);
        return count == null ? 0 : count;
    }
}
