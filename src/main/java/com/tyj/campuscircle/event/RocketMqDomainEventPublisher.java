package com.tyj.campuscircle.event;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Profile("rocketmq")
public class RocketMqDomainEventPublisher implements DomainEventPublisher {

    public static final String HEADER_EVENT_TYPE = "campuscircle-event-type";
    public static final String HEADER_COMMENT_ID = "campuscircle-comment-id";
    public static final String HEADER_POST_ID = "campuscircle-post-id";
    public static final String HEADER_SENDER_ID = "campuscircle-sender-id";
    public static final String EVENT_COMMENT_CREATED = "COMMENT_CREATED";
    public static final String EVENT_POST_LIKED = "POST_LIKED";

    private final RocketMQTemplate rocketMQTemplate;
    private final String commentTopic;
    private final String likeTopic;

    public RocketMqDomainEventPublisher(
            RocketMQTemplate rocketMQTemplate,
            @Value("${campuscircle.rocketmq.comment-topic}") String commentTopic,
            @Value("${campuscircle.rocketmq.like-topic}") String likeTopic) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.commentTopic = commentTopic;
        this.likeTopic = likeTopic;
    }

    @Override
    public void publishCommentCreated(CommentCreatedEvent event) {
        Message<CommentCreatedEvent> message = MessageBuilder.withPayload(event)
                .setHeader(HEADER_EVENT_TYPE, EVENT_COMMENT_CREATED)
                .setHeader(HEADER_COMMENT_ID, event.commentId())
                .build();
        rocketMQTemplate.sendMessageInTransaction(commentTopic, message, null);
    }

    @Override
    public void publishPostLiked(PostLikedEvent event) {
        Message<PostLikedEvent> message = MessageBuilder.withPayload(event)
                .setHeader(HEADER_EVENT_TYPE, EVENT_POST_LIKED)
                .setHeader(HEADER_POST_ID, event.postId())
                .setHeader(HEADER_SENDER_ID, event.senderId())
                .build();
        rocketMQTemplate.sendMessageInTransaction(likeTopic, message, null);
    }
}
