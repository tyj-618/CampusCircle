package com.tyj.campuscircle.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.PostStatEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ViewCountMapper extends BaseMapper<PostStatEntity> {

    @Update("""
            UPDATE post_stat
            SET view_count = view_count + #{delta},
                hot_score = hot_score + #{delta}
            WHERE post_id = #{postId}
            """)
    void increaseViewCount(@Param("postId") Long postId, @Param("delta") long delta);
}
