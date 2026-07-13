package com.tyj.campuscircle.post;

import java.util.List;

public record PageQueryResult<T>(
        long total,
        List<T> records
) {
}
