package com.tyj.campuscircle.post;

import com.tyj.campuscircle.common.ApiResponse;
import com.tyj.campuscircle.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/posts")
    public ApiResponse<CreatePostResponse> createPost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.createPost(authorization, request));
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<PostListItemResponse>> listPosts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort) {
        return ApiResponse.success(postService.listPosts(page, size, categoryId, keyword, sort));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(postService.getPostDetail(postId, authorization));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<Boolean> updatePost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdatePostRequest request) {
        postService.updatePost(postId, authorization, request);
        return ApiResponse.success(true);
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Boolean> deletePost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        postService.deletePost(postId, authorization);
        return ApiResponse.success(true);
    }

    @GetMapping("/users/{userId}/posts")
    public ApiResponse<PageResponse<PostListItemResponse>> listUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.success(postService.listUserPosts(userId, page, size));
    }

    @GetMapping("/posts/hot")
    public ApiResponse<List<PostHotItemResponse>> listHotPosts(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(postService.listHotPosts(limit, categoryId));
    }
}
