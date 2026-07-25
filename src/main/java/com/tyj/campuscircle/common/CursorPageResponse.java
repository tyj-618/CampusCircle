package com.tyj.campuscircle.common;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> records,
        String nextCursor,
        boolean hasMore
) {
}
