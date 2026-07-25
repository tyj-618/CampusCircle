package com.tyj.campuscircle.post;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ViewCountServiceTests {

    @Test
    void requeuesViewDeltaWhenDatabaseFlushFails() {
        ViewCountMapper viewCountMapper = mock(ViewCountMapper.class);
        HotPostRankStore hotPostRankStore = mock(HotPostRankStore.class);
        ViewCountService service = new ViewCountService(viewCountMapper, hotPostRankStore, 60);
        doThrow(new IllegalStateException("database unavailable"))
                .doNothing()
                .when(viewCountMapper)
                .increaseViewCount(1L, 1L);

        service.recordView(1L, 2L);

        assertThatThrownBy(service::flushPendingViews)
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(service::flushPendingViews)
                .doesNotThrowAnyException();

        verify(viewCountMapper, times(2)).increaseViewCount(1L, 1L);
        verify(hotPostRankStore).increaseScore(1L, 2L, HotPostRankStore.VIEW_SCORE);
        verifyNoMoreInteractions(viewCountMapper, hotPostRankStore);
    }
}
