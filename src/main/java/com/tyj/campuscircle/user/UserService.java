package com.tyj.campuscircle.user;

import com.tyj.campuscircle.auth.CurrentUserService;
import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;

    public UserService(CurrentUserService currentUserService, UserMapper userMapper) {
        this.currentUserService = currentUserService;
        this.userMapper = userMapper;
    }

    public UserProfileResponse getCurrentUser(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        UserProfile userProfile = findExistingUser(currentUserId);
        return UserProfileResponse.from(userProfile);
    }

    public UserProfileResponse updateCurrentUser(String authorization, UpdateUserProfileRequest request) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        UserProfile oldProfile = findExistingUser(currentUserId);

        String nickname = cleanOrDefault(request.nickname(), oldProfile.nickname());
        String avatarUrl = cleanOrDefault(request.avatarUrl(), oldProfile.avatarUrl());
        String bio = cleanOrDefault(request.bio(), oldProfile.bio());

        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称不能为空");
        }

        userMapper.updateProfile(currentUserId, nickname, avatarUrl, bio);

        UserProfile updatedProfile = findExistingUser(currentUserId);
        return UserProfileResponse.from(updatedProfile);
    }

    public PublicUserProfileResponse getPublicUserProfile(Long userId) {
        UserProfile userProfile = findExistingUser(userId);
        long postCount = userMapper.countNormalPostsByUserId(userId);
        long commentCount = userMapper.countNormalCommentsByUserId(userId);

        return new PublicUserProfileResponse(
                userProfile.id(),
                userProfile.username(),
                userProfile.nickname(),
                userProfile.avatarUrl(),
                userProfile.bio(),
                postCount,
                commentCount,
                userProfile.createdAt()
        );
    }

    private UserProfile findExistingUser(Long userId) {
        return userMapper.findProfileById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    private String cleanOrDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        String cleanedValue = value.trim();
        return cleanedValue.isEmpty() ? null : cleanedValue;
    }
}
