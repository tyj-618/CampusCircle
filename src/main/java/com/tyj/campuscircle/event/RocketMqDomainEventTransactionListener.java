package com.tyj.campuscircle.event;

import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

@Profile("rocketmq")
@RocketMQTransactionListener
public class RocketMqDomainEventTransactionListener implements RocketMQLocalTransactionListener {

    private final DomainEventTransactionMapper transactionMapper;

    public RocketMqDomainEventTransactionListener(DomainEventTransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object arg) {
        return RocketMQLocalTransactionState.UNKNOWN;
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        String eventType = headerAsString(message, RocketMqDomainEventPublisher.HEADER_EVENT_TYPE);
        if (RocketMqDomainEventPublisher.EVENT_COMMENT_CREATED.equals(eventType)) {
            Long commentId = headerAsLong(message, RocketMqDomainEventPublisher.HEADER_COMMENT_ID);
            return transactionMapper.existsNormalComment(commentId)
                    ? RocketMQLocalTransactionState.COMMIT
                    : RocketMQLocalTransactionState.ROLLBACK;
        }
        if (RocketMqDomainEventPublisher.EVENT_POST_LIKED.equals(eventType)) {
            Long postId = headerAsLong(message, RocketMqDomainEventPublisher.HEADER_POST_ID);
            Long senderId = headerAsLong(message, RocketMqDomainEventPublisher.HEADER_SENDER_ID);
            return transactionMapper.existsActiveLike(postId, senderId)
                    ? RocketMQLocalTransactionState.COMMIT
                    : RocketMQLocalTransactionState.ROLLBACK;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }

    private String headerAsString(Message message, String name) {
        Object value = message.getHeaders().get(name);
        return value == null ? null : value.toString();
    }

    private Long headerAsLong(Message message, String name) {
        Object value = message.getHeaders().get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            return Long.valueOf(value.toString());
        }
        return null;
    }
}
