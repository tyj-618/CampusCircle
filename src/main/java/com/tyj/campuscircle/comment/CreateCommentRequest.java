package com.tyj.campuscircle.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论内容不能超过 500 位")
        String content
) {
}
