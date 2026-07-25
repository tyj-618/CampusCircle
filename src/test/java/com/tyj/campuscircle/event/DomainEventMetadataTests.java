package com.tyj.campuscircle.event;

import com.tyj.campuscircle.notice.NoticeService;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DomainEventMetadataTests {

    @Test
    void syncPublisherPassesStableEventIdToNoticeService() {
        NoticeService noticeService = mock(NoticeService.class);
        SyncDomainEventPublisher publisher = new SyncDomainEventPublisher(noticeService);
        PostLikedEvent event = new PostLikedEvent(
                "event-1", Instant.parse("2026-07-26T00:00:00Z"), 1L, 2L, 3L
        );

        publisher.publishPostLiked(event);

        verify(noticeService).createLikeNotice("event-1", 1L, 2L, 3L);
    }
}
