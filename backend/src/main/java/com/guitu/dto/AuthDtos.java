package com.guitu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "账号不能为空")
            @Size(max = 64, message = "账号长度不能超过64")
            String account,

            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需为6-32位")
            String password,

            @NotBlank(message = "确认密码不能为空")
            String confirmPassword,

            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String phone,

            @NotBlank(message = "昵称不能为空")
            @Size(max = 64, message = "昵称长度不能超过64")
            String nickname
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "账号不能为空")
            String account,

            @NotBlank(message = "密码不能为空")
            String password
    ) {
    }

    public record AuthResponse(String token, UserDtos.UserProfile user) {
    }
}
