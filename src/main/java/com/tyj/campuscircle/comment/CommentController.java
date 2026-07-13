package com.tyj.campuscircle.comment;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CreateCommentResponse> createComment(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success(commentService.createComment(postId, authorization, request));
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> listPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.success(commentService.listPostComments(postId, page, size));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Boolean> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        commentService.deleteComment(commentId, authorization);
        return ApiResponse.success(true);
    }

    @GetMapping("/users/me/comments")
    public ApiResponse<PageResponse<MyCommentResponse>> listMyComments(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.success(commentService.listMyComments(authorization, page, size));
    }
}
