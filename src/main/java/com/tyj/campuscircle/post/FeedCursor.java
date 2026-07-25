package com.tyj.campuscircle.post;

import java.time.LocalDateTime;

public record FeedCursor(
        LocalDateTime createdAt,
        Long id
) {
}
