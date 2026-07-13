package com.tyj.campuscircle.category;

import java.time.LocalDateTime;

public record Category(
        Long id,
        String name,
        String code,
        Integer sortOrder,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
