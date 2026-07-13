package com.tyj.campuscircle.like;

import com.tyj.campuscircle.auth.CurrentUserService;
import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.event.DomainEventPublisher;
import com.tyj.campuscircle.event.PostLikedEvent;
import com.tyj.campuscircle.exception.BusinessException;
import com.tyj.campuscircle.post.HotPostRankStore;
import com.tyj.campuscircle.post.PostDetail;
import com.tyj.campuscircle.post.PostMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final CurrentUserService currentUserService;
    private final LikeMapper likeMapper;
    private final PostMapper postMapper;
    private final HotPostRankStore hotPostRankStore;
    private final DomainEventPublisher domainEventPublisher;

    public LikeService(CurrentUserService currentUserService, LikeMapper likeMapper, PostMapper postMapper,
                       HotPostRankStore hotPostRankStore, DomainEventPublisher domainEventPublisher) {
        this.currentUserService = currentUserService;
        this.likeMapper = likeMapper;
        this.postMapper = postMapper;
        this.hotPostRankStore = hotPostRankStore;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public LikeResponse likePost(Long postId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        PostDetail postDetail = findNormalPost(postId);

        LikeRecord likeRecord = likeMapper.findByPostIdAndUserId(postId, currentUserId).orElse(null);
        if (likeRecord != null && likeRecord.status() == 0) {
            return new LikeResponse(true, likeMapper.findLikeCount(postId));
        }

        if (likeRecord == null) {
            try {
                likeMapper.saveLike(postId, currentUserId);
            } catch (DuplicateKeyException exception) {
                likeRecord = likeMapper.findByPostIdAndUserId(postId, currentUserId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "点赞状态查询失败"));
                if (likeRecord.status() == 0) {
                    return new LikeResponse(true, likeMapper.findLikeCount(postId));
                }
                likeMapper.activateLike(likeRecord.id());
            }
        } else {
            likeMapper.activateLike(likeRecord.id());
        }

        likeMapper.increaseLikeCount(postId);
        hotPostRankStore.increaseScore(postId, postDetail.categoryId(), HotPostRankStore.LIKE_SCORE);
        domainEventPublisher.publishPostLiked(new PostLikedEvent(postDetail.userId(), currentUserId, postId));
        return new LikeResponse(true, likeMapper.findLikeCount(postId));
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        PostDetail postDetail = findNormalPost(postId);

        LikeRecord likeRecord = likeMapper.findByPostIdAndUserId(postId, currentUserId).orElse(null);
        if (likeRecord == null || likeRecord.status() == 1) {
            return new LikeResponse(false, likeMapper.findLikeCount(postId));
        }

        likeMapper.cancelLike(likeRecord.id());
        likeMapper.decreaseLikeCount(postId);
        hotPostRankStore.decreaseScore(postId, postDetail.categoryId(), HotPostRankStore.LIKE_SCORE);
        return new LikeResponse(false, likeMapper.findLikeCount(postId));
    }

    public LikeStatusResponse getLikeStatus(Long postId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        ensureNormalPost(postId);

        boolean liked = likeMapper.findByPostIdAndUserId(postId, currentUserId)
                .map(likeRecord -> likeRecord.status() == 0)
                .orElse(false);

        return new LikeStatusResponse(liked);
    }

    private void ensureNormalPost(Long postId) {
        if (!likeMapper.existsNormalPost(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
    }

    private PostDetail findNormalPost(Long postId) {
        PostDetail postDetail = postMapper.findDetailById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在"));

        if (postDetail.status() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }

        return postDetail;
    }
}
