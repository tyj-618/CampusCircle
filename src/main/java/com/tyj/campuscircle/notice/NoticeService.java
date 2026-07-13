package com.tyj.campuscircle.notice;

import com.tyj.campuscircle.auth.CurrentUserService;
import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.common.PageResponse;
import com.tyj.campuscircle.exception.BusinessException;
import com.tyj.campuscircle.post.PageQueryResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_LIKE = 2;

    private final CurrentUserService currentUserService;
    private final NoticeMapper noticeMapper;

    public NoticeService(CurrentUserService currentUserService, NoticeMapper noticeMapper) {
        this.currentUserService = currentUserService;
        this.noticeMapper = noticeMapper;
    }

    public void createCommentNotice(Long receiverId, Long senderId, Long postId, Long commentId) {
        if (receiverId.equals(senderId)) {
            return;
        }

        noticeMapper.saveNotice(new CreateNoticeCommand(
                receiverId,
                senderId,
                postId,
                commentId,
                TYPE_COMMENT,
                buildCommentEventKey(receiverId, senderId, postId, commentId),
                "你的帖子收到了新的评论"
        ));
    }

    public void createLikeNotice(Long receiverId, Long senderId, Long postId) {
        if (receiverId.equals(senderId)) {
            return;
        }

        noticeMapper.saveNotice(new CreateNoticeCommand(
                receiverId,
                senderId,
                postId,
                null,
                TYPE_LIKE,
                buildLikeEventKey(receiverId, senderId, postId),
                "你的帖子收到了新的点赞"
        ));
    }

    public PageResponse<NoticeResponse> listNotices(String authorization, int page, int size, Integer readStatus) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        validateReadStatus(readStatus);

        PageQueryResult<NoticeItem> result = noticeMapper.findNoticesByReceiverId(currentUserId, page, size, readStatus);
        List<NoticeResponse> records = result.records().stream()
                .map(NoticeResponse::from)
                .toList();

        return PageResponse.of(page, size, result.total(), records);
    }

    public UnreadNoticeCountResponse countUnreadNotices(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        return new UnreadNoticeCountResponse(noticeMapper.countUnreadByReceiverId(currentUserId));
    }

    public void markNoticeRead(Long noticeId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        int updatedCount = noticeMapper.markRead(noticeId, currentUserId);

        if (updatedCount == 0 && !noticeMapper.existsByIdAndReceiverId(noticeId, currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
    }

    public UpdateNoticeCountResponse markAllNoticesRead(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        int updatedCount = noticeMapper.markAllRead(currentUserId);
        return new UpdateNoticeCountResponse(updatedCount);
    }

    private void validateReadStatus(Integer readStatus) {
        if (readStatus != null && readStatus != 0 && readStatus != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "通知阅读状态只能是 0 或 1");
        }
    }

    private String buildCommentEventKey(Long receiverId, Long senderId, Long postId, Long commentId) {
        return "comment:" + receiverId + ":" + senderId + ":" + postId + ":" + commentId;
    }

    private String buildLikeEventKey(Long receiverId, Long senderId, Long postId) {
        return "like:" + receiverId + ":" + senderId + ":" + postId;
    }
}
