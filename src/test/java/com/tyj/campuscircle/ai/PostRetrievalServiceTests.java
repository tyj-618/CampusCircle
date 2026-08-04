package com.tyj.campuscircle.ai;

import com.tyj.campuscircle.post.PostListItem;
import com.tyj.campuscircle.post.PostMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostRetrievalServiceTests {

    @Test
    void skipsDatabaseLookupWhenTheCallerHasNoPermittedSchool() {
        PostMapper postMapper = mock(PostMapper.class);
        PostRetrievalService service = new PostRetrievalService(postMapper);

        List<RetrievedPost> posts = service.retrieve(new RetrievalQuery("图书馆开放时间", List.of(), 5));

        assertThat(posts).isEmpty();
        verify(postMapper, never()).findPostsBySchoolIdsAndKeyword(
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString(), anyInt());
    }

    @Test
    void keepsTheAllowedSchoolScopeWhenRetrievingPosts() {
        PostMapper postMapper = mock(PostMapper.class);
        PostRetrievalService service = new PostRetrievalService(postMapper);
        List<Long> allowedSchoolIds = List.of(1L, 2L);
        when(postMapper.findPostsBySchoolIdsAndKeyword(eq(allowedSchoolIds), eq("图书馆开放时间"), eq(3)))
                .thenReturn(List.of(post(11L)));

        List<RetrievedPost> posts = service.retrieve(new RetrievalQuery("图书馆开放时间", allowedSchoolIds, 3));

        assertThat(posts).extracting(RetrievedPost::id).containsExactly(11L);
        verify(postMapper).findPostsBySchoolIdsAndKeyword(allowedSchoolIds, "图书馆开放时间", 3);
    }

    @Test
    void rejectsANonPositiveRetrievalLimit() {
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new RetrievalQuery("问题", List.of(1L), 0))
                .withMessage("limit must be positive");
    }

    private PostListItem post(Long id) {
        return new PostListItem(id, "图书馆开放时间", "晚上十点闭馆", 1L, "示例大学", "南京", 1L,
                "学习", "study", 1L, "测试用户", null, 0, 0, 0, 0D, LocalDateTime.now());
    }
}
