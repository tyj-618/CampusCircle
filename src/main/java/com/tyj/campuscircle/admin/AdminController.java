package com.tyj.campuscircle.admin;

import com.tyj.campuscircle.common.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PutMapping("/posts/{postId}/hide")
    public ApiResponse<Boolean> hidePost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        adminService.hidePost(postId, authorization);
        return ApiResponse.success(true);
    }

    @PutMapping("/posts/{postId}/restore")
    public ApiResponse<Boolean> restorePost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        adminService.restorePost(postId, authorization);
        return ApiResponse.success(true);
    }

    @PutMapping("/users/{userId}/disable")
    public ApiResponse<Boolean> disableUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        adminService.disableUser(userId, authorization);
        return ApiResponse.success(true);
    }

    @PutMapping("/users/{userId}/enable")
    public ApiResponse<Boolean> enableUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        adminService.enableUser(userId, authorization);
        return ApiResponse.success(true);
    }
}
