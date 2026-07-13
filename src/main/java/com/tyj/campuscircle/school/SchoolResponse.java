package com.tyj.campuscircle.school;

import com.tyj.campuscircle.common.entity.SchoolEntity;

import java.math.BigDecimal;

public record SchoolResponse(
        Long id,
        String name,
        String province,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceKm
) {

    public static SchoolResponse from(SchoolEntity school) {
        return from(school, null);
    }

    public static SchoolResponse from(SchoolEntity school, Double distanceKm) {
        return new SchoolResponse(
                school.getId(),
                school.getName(),
                school.getProvince(),
                school.getCity(),
                school.getLatitude(),
                school.getLongitude(),
                distanceKm
        );
    }
}
