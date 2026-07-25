package com.tyj.campuscircle.post;

import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class FeedCursorCodec {

    private FeedCursorCodec() {
    }

    public static String encode(FeedCursor cursor) {
        String raw = cursor.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "|" + cursor.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static FeedCursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }

            LocalDateTime createdAt = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long id = Long.parseLong(parts[1]);
            if (id <= 0) {
                throw new IllegalArgumentException();
            }
            return new FeedCursor(createdAt, id);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分页游标无效");
        }
    }
}
