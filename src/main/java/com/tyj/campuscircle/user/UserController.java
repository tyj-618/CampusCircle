package com.tyj.campuscircle.user;

import com.tyj.campuscircle.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(userService.getCurrentUser(authorization));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userService.updateCurrentUser(authorization, request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<PublicUserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ApiResponse.success(userService.getPublicUserProfile(userId));
    }
}
