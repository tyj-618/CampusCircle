package com.tyj.campuscircle.comment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.CommentEntity;
import com.tyj.campuscircle.post.PageQueryResult;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface CommentMapper extends BaseMapper<CommentEntity> {

    @Select("SELECT COUNT(*) FROM post WHERE id = #{postId} AND status = 0")
    long countNormalPost(@Param("postId") Long postId);

    default boolean existsNormalPost(Long postId) {
        return countNormalPost(postId) > 0;
    }

    default Long saveComment(Long postId, Long userId, String content) {
        CommentEntity comment = new CommentEntity();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        insert(comment);
        return comment.getId();
    }

    @Update("""
            UPDATE post_stat
            SET comment_count = comment_count + 1,
                hot_score = hot_score + 3
            WHERE post_id = #{postId}
            """)
    void increaseCommentCount(@Param("postId") Long postId);

    @Update("""
            UPDATE post_stat
            SET comment_count = GREATEST(comment_count - 1, 0),
                hot_score = GREATEST(hot_score - 3, 0)
            WHERE post_id = #{postId}
            """)
    void decreaseCommentCount(@Param("postId") Long postId);

    @Select("SELECT COUNT(*) FROM `comment` WHERE post_id = #{postId} AND status = 0")
    long countCommentsByPostId(@Param("postId") Long postId);

    @Select("""
            SELECT c.id, c.post_id AS postId, c.user_id AS userId, c.content,
                   u.nickname AS authorNickname, u.avatar_url AS authorAvatarUrl,
                   c.created_at AS createdAt
            FROM `comment` c
            JOIN `user` u ON c.user_id = u.id
            WHERE c.post_id = #{postId} AND c.status = 0
            ORDER BY c.created_at ASC, c.id ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<CommentPageItem> selectCommentsByPostId(@Param("postId") Long postId,
                                                 @Param("limit") int limit,
                                                 @Param("offset") int offset);

    default PageQueryResult<CommentPageItem> findCommentsByPostId(Long postId, int page, int size) {
        long total = countCommentsByPostId(postId);
        List<CommentPageItem> records = selectCommentsByPostId(postId, size, (page - 1) * size);
        return new PageQueryResult<>(total, records);
    }

    @Select("SELECT COUNT(*) FROM `comment` WHERE user_id = #{userId} AND status = 0")
    long countCommentsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT c.id, c.post_id AS postId, p.title AS postTitle, c.content,
                   c.created_at AS createdAt
            FROM `comment` c
            JOIN post p ON c.post_id = p.id
            WHERE c.user_id = #{userId} AND c.status = 0
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<MyCommentItem> selectCommentsByUserId(@Param("userId") Long userId,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    default PageQueryResult<MyCommentItem> findCommentsByUserId(Long userId, int page, int size) {
        long total = countCommentsByUserId(userId);
        List<MyCommentItem> records = selectCommentsByUserId(userId, size, (page - 1) * size);
        return new PageQueryResult<>(total, records);
    }

    @Select("""
            SELECT c.id, c.post_id AS postId, c.user_id AS userId, p.user_id AS postAuthorId,
                   c.content, c.status, c.created_at AS createdAt, c.updated_at AS updatedAt
            FROM `comment` c
            JOIN post p ON c.post_id = p.id
            WHERE c.id = #{commentId}
            """)
    CommentDetail selectDetailById(@Param("commentId") Long commentId);

    default Optional<CommentDetail> findDetailById(Long commentId) {
        return Optional.ofNullable(selectDetailById(commentId));
    }

    @Update("UPDATE `comment` SET status = 1 WHERE id = #{commentId}")
    void softDeleteComment(@Param("commentId") Long commentId);
}
