package com.tyj.campuscircle.category;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.CategoryEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CategoryMapper extends BaseMapper<CategoryEntity> {

    @Select("""
            SELECT id, name, code, sort_order AS sortOrder, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM category
            WHERE status = 0
            ORDER BY sort_order ASC, id ASC
            """)
    List<Category> findEnabledCategories();
}
