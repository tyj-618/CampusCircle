package com.tyj.campuscircle.like;

import com.tyj.campuscircle.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/like")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    public ApiResponse<LikeResponse> likePost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(likeService.likePost(postId, authorization));
    }

    @DeleteMapping
    public ApiResponse<LikeResponse> unlikePost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(likeService.unlikePost(postId, authorization));
    }

    @GetMapping
    public ApiResponse<LikeStatusResponse> getLikeStatus(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(likeService.getLikeStatus(postId, authorization));
    }
}
