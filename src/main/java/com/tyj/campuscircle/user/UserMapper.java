package com.tyj.campuscircle.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.UserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("""
            SELECT u.id, u.username, u.nickname, u.school_id AS schoolId,
                   s.name AS schoolName, s.city AS schoolCity,
                   u.avatar_url AS avatarUrl, u.bio, u.role, u.status,
                   u.created_at AS createdAt, u.updated_at AS updatedAt
            FROM `user` u
            JOIN school s ON u.school_id = s.id
            WHERE u.id = #{userId}
            """)
    Optional<UserProfile> findProfileById(@Param("userId") Long userId);

    @Update("""
            UPDATE `user`
            SET nickname = #{nickname}, avatar_url = #{avatarUrl}, bio = #{bio}, school_id = #{schoolId}
            WHERE id = #{userId}
            """)
    void updateProfile(
            @Param("userId") Long userId,
            @Param("nickname") String nickname,
            @Param("avatarUrl") String avatarUrl,
            @Param("bio") String bio,
            @Param("schoolId") Long schoolId);

    @Select("SELECT COUNT(*) FROM post WHERE user_id = #{userId} AND status = 0")
    long countNormalPostsByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM `comment` WHERE user_id = #{userId} AND status = 0")
    long countNormalCommentsByUserId(@Param("userId") Long userId);
}
