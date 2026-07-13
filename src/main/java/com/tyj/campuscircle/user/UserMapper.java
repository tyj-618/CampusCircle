package com.tyj.campuscircle.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("""
            SELECT id, username, nickname, avatar_url AS avatarUrl, bio, role, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM `user`
            WHERE id = #{userId}
            """)
    Optional<UserProfile> findProfileById(@Param("userId") Long userId);

    @Update("""
            UPDATE `user`
            SET nickname = #{nickname}, avatar_url = #{avatarUrl}, bio = #{bio}
            WHERE id = #{userId}
            """)
    void updateProfile(
            @Param("userId") Long userId,
            @Param("nickname") String nickname,
            @Param("avatarUrl") String avatarUrl,
            @Param("bio") String bio);

    @Select("SELECT COUNT(*) FROM post WHERE user_id = #{userId} AND status = 0")
    long countNormalPostsByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM `comment` WHERE user_id = #{userId} AND status = 0")
    long countNormalCommentsByUserId(@Param("userId") Long userId);
}
