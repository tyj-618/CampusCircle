package com.tyj.campuscircle.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AdminMapper extends BaseMapper<UserEntity> {

    @Select("SELECT COUNT(*) FROM post WHERE id = #{postId}")
    long countPostById(@Param("postId") Long postId);

    default boolean existsPost(Long postId) {
        return countPostById(postId) > 0;
    }

    @Update("UPDATE post SET status = #{status} WHERE id = #{postId}")
    void updatePostStatus(@Param("postId") Long postId, @Param("status") int status);

    @Select("SELECT COUNT(*) FROM `user` WHERE id = #{userId}")
    long countUserById(@Param("userId") Long userId);

    default boolean existsUser(Long userId) {
        return countUserById(userId) > 0;
    }

    @Update("UPDATE `user` SET status = #{status} WHERE id = #{userId}")
    void updateUserStatus(@Param("userId") Long userId, @Param("status") int status);
}
