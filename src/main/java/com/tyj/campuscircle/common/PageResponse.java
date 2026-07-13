package com.tyj.campuscircle.common;

import java.util.List;

public record PageResponse<T>(
        int page,
        int size,
        long total,
        long pages,
        List<T> records
) {

    public static <T> PageResponse<T> of(int page, int size, long total, List<T> records) {
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResponse<>(page, size, total, pages, records);
    }
}
