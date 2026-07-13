package com.tyj.campuscircle.school;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyj.campuscircle.common.entity.SchoolEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

public interface SchoolMapper extends BaseMapper<SchoolEntity> {

    @Select("""
            SELECT id, name, province, city, latitude, longitude, status
            FROM school
            WHERE id = #{schoolId} AND status = 0
            """)
    Optional<SchoolEntity> findEnabledById(@Param("schoolId") Long schoolId);

    @Select("""
            SELECT id, name, province, city, latitude, longitude, status
            FROM school
            WHERE status = 0
            ORDER BY city, name
            """)
    List<SchoolEntity> findEnabledSchools();

    @Select("""
            SELECT id, name, province, city, latitude, longitude, status
            FROM school
            WHERE status = 0 AND city = #{city}
            ORDER BY name
            """)
    List<SchoolEntity> findEnabledByCity(@Param("city") String city);
}
