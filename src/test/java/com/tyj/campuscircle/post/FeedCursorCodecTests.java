package com.tyj.campuscircle.post;

import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedCursorCodecTests {

    @Test
    void encodesAndDecodesCursor() {
        FeedCursor original = new FeedCursor(LocalDateTime.of(2026, 7, 23, 14, 30), 1024L);

        FeedCursor decoded = FeedCursorCodec.decode(FeedCursorCodec.encode(original));

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void rejectsInvalidCursor() {
        assertThatThrownBy(() -> FeedCursorCodec.decode("not-a-valid-cursor"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }
}
