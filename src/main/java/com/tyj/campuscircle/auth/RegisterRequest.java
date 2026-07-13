package com.tyj.campuscircle.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度必须在 3 到 32 位之间")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 位之间")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称长度不能超过 32 位")
        String nickname
) {
}
