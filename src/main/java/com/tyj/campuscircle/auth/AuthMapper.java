package com.tyj.campuscircle.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

public interface AuthMapper extends BaseMapper<UserEntity> {

    @Select("SELECT COUNT(*) FROM `user` WHERE username = #{username}")
    long countByUsername(@Param("username") String username);

    default boolean existsByUsername(String username) {
        return countByUsername(username) > 0;
    }

    @Insert("""
            INSERT INTO `user` (username, password, nickname)
            VALUES (#{username}, #{encodedPassword}, #{nickname})
            """)
    void save(
            @Param("username") String username,
            @Param("encodedPassword") String encodedPassword,
            @Param("nickname") String nickname);

    @Select("""
            SELECT id, username, password, nickname, avatar_url AS avatarUrl, role, status
            FROM `user`
            WHERE username = #{username}
            """)
    Optional<AuthUser> findByUsername(@Param("username") String username);
}
