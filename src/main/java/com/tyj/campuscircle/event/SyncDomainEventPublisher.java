package com.tyj.campuscircle.event;

import com.tyj.campuscircle.notice.NoticeService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!rocketmq")
public class SyncDomainEventPublisher implements DomainEventPublisher {

    private final NoticeService noticeService;

    public SyncDomainEventPublisher(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public void publishCommentCreated(CommentCreatedEvent event) {
        noticeService.createCommentNotice(event.receiverId(), event.senderId(), event.postId(), event.commentId());
    }

    @Override
    public void publishPostLiked(PostLikedEvent event) {
        noticeService.createLikeNotice(event.receiverId(), event.senderId(), event.postId());
    }
}
