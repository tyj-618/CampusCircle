package com.tyj.campuscircle.admin;

import com.tyj.campuscircle.auth.CurrentUserService;
import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;
import com.tyj.campuscircle.user.UserProfile;
import com.tyj.campuscircle.user.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private static final int USER_STATUS_NORMAL = 0;
    private static final int USER_STATUS_DISABLED = 1;
    private static final int POST_STATUS_NORMAL = 0;
    private static final int POST_STATUS_HIDDEN = 2;

    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;

    public AdminService(CurrentUserService currentUserService, UserMapper userMapper, AdminMapper adminMapper) {
        this.currentUserService = currentUserService;
        this.userMapper = userMapper;
        this.adminMapper = adminMapper;
    }

    public void hidePost(Long postId, String authorization) {
        requireAdmin(authorization);
        ensurePostExists(postId);
        adminMapper.updatePostStatus(postId, POST_STATUS_HIDDEN);
    }

    public void restorePost(Long postId, String authorization) {
        requireAdmin(authorization);
        ensurePostExists(postId);
        adminMapper.updatePostStatus(postId, POST_STATUS_NORMAL);
    }

    public void disableUser(Long userId, String authorization) {
        Long currentUserId = requireAdmin(authorization);
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能禁用当前登录的管理员账号");
        }

        ensureUserExists(userId);
        adminMapper.updateUserStatus(userId, USER_STATUS_DISABLED);
    }

    public void enableUser(Long userId, String authorization) {
        requireAdmin(authorization);
        ensureUserExists(userId);
        adminMapper.updateUserStatus(userId, USER_STATUS_NORMAL);
    }

    private Long requireAdmin(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        UserProfile currentUser = userMapper.findProfileById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (currentUser.status() != USER_STATUS_NORMAL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }

        if (currentUser.role() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }

        return currentUserId;
    }

    private void ensurePostExists(Long postId) {
        if (!adminMapper.existsPost(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
    }

    private void ensureUserExists(Long userId) {
        if (!adminMapper.existsUser(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }
}
