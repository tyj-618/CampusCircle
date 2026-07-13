package com.tyj.campuscircle.auth;

import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, TokenStore tokenStore) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (authMapper.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        try {
            authMapper.save(request.username(), encodedPassword, request.nickname());
        } catch (DuplicateKeyException exception) { // 防止并发情况
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        AuthUser user = authMapper.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "用户注册后查询失败"));

        return new RegisterResponse(user.id(), user.username(), user.nickname());
    }

    public LoginResponse login(LoginRequest request) {
        AuthUser user = authMapper.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FAILED));

        if (user.status() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }

        if (!passwordEncoder.matches(request.password(), user.password())) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }

        TokenSession session = tokenStore.createSession(user.id());
        UserSummary userSummary = new UserSummary(
                user.id(),
                user.username(),
                user.nickname(),
                user.avatarUrl(),
                user.role()
        );

        return new LoginResponse(session.token(), session.expiresIn(), userSummary);
    }

    public void logout(String authorization) {
        String token = tokenStore.resolveBearerToken(authorization)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        tokenStore.remove(token);
    }
}
