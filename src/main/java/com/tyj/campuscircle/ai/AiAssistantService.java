package com.tyj.campuscircle.ai;

import com.tyj.campuscircle.auth.CurrentUserService;
import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;
import com.tyj.campuscircle.school.SchoolService;
import com.tyj.campuscircle.user.UserMapper;
import com.tyj.campuscircle.user.UserProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    private static final int RETRIEVAL_LIMIT = 5;
    private static final double DEFAULT_RADIUS_KM = 5.0;

    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;
    private final SchoolService schoolService;
    private final PostRetrievalService postRetrievalService;
    private final PromptBuilder promptBuilder;
    private final AiModelClient aiModelClient;
    private final AiRequestRateLimiter aiRequestRateLimiter;

    public AiAssistantService(CurrentUserService currentUserService, UserMapper userMapper,
                              SchoolService schoolService, PostRetrievalService postRetrievalService,
                              PromptBuilder promptBuilder, AiModelClient aiModelClient,
                              AiRequestRateLimiter aiRequestRateLimiter) {
        this.currentUserService = currentUserService;
        this.userMapper = userMapper;
        this.schoolService = schoolService;
        this.postRetrievalService = postRetrievalService;
        this.promptBuilder = promptBuilder;
        this.aiModelClient = aiModelClient;
        this.aiRequestRateLimiter = aiRequestRateLimiter;
    }

    public AiAssistantResponse ask(String authorization, AiAssistantRequest request) {
        Long userId = currentUserService.requireUserId(authorization);
        aiRequestRateLimiter.check(userId);
        UserProfile user = userMapper.findProfileById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        double radiusKm = request.radiusKm() == null ? DEFAULT_RADIUS_KM : request.radiusKm();
        List<Long> allowedSchoolIds = schoolService.listNearbySchoolIds(user.schoolId(), radiusKm);
        List<RetrievedPost> posts = postRetrievalService.retrieve(request.question(), allowedSchoolIds, RETRIEVAL_LIMIT);
        if (posts.isEmpty()) {
            return new AiAssistantResponse(
                    "在当前查看范围内暂未找到相关校园帖子。",
                    List.of(), true, UUID.randomUUID().toString());
        }

        AiModelResult result = aiModelClient.generate(promptBuilder.build(request.question(), posts));
        Map<Long, RetrievedPost> postsById = posts.stream()
                .collect(Collectors.toMap(RetrievedPost::id, Function.identity()));
        Set<Long> validPostIds = result.citedPostIds().stream()
                .filter(postsById::containsKey)
                .collect(Collectors.toSet());
        List<AiPostReference> references = posts.stream()
                .filter(post -> validPostIds.contains(post.id()))
                .map(AiPostReference::from)
                .toList();

        return new AiAssistantResponse(
                result.answer(),
                references,
                result.insufficientEvidence() || references.isEmpty(),
                result.requestId());
    }
}
