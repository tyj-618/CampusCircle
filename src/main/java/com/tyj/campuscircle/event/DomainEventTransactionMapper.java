package com.tyj.campuscircle.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.CommentEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DomainEventTransactionMapper extends BaseMapper<CommentEntity> {

    @Select("SELECT COUNT(*) FROM `comment` WHERE id = #{commentId} AND status = 0")
    long countNormalComment(@Param("commentId") Long commentId);

    default boolean existsNormalComment(Long commentId) {
        return countNormalComment(commentId) > 0;
    }

    @Select("""
            SELECT COUNT(*)
            FROM post_like
            WHERE post_id = #{postId} AND user_id = #{senderId} AND status = 0
            """)
    long countActiveLike(@Param("postId") Long postId, @Param("senderId") Long senderId);

    default boolean existsActiveLike(Long postId, Long senderId) {
        return countActiveLike(postId, senderId) > 0;
    }
}
