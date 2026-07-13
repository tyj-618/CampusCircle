package com.tyj.campuscircle.event;

public interface DomainEventPublisher {

    void publishCommentCreated(CommentCreatedEvent event);

    void publishPostLiked(PostLikedEvent event);
}
