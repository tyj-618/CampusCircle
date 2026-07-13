package com.tyj.campuscircle.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotNull(message = "分类不能为空")
        Long categoryId,

        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题长度不能超过 100 位")
        String title,

        @NotBlank(message = "正文不能为空")
        @Size(max = 5000, message = "正文长度不能超过 5000 位")
        String content
) {
}
