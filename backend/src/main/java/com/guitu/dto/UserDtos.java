package com.guitu.dto;

import com.guitu.domain.enums.UserRole;
import com.guitu.domain.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserProfile(
            Long id,
            String account,
            String nickname,
            String phone,
            String avatarUrl,
            UserRole role,
            String roleText,
            UserStatus status,
            String statusText,
            LocalDateTime createdAt
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "昵称不能为空")
            @Size(max = 64, message = "昵称长度不能超过64")
            String nickname,

            @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String phone,

            @Size(max = 500, message = "头像路径长度不能超过500")
            String avatarUrl
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "原密码不能为空")
            String oldPassword,

            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需为6-32位")
            String newPassword,

            @NotBlank(message = "确认密码不能为空")
            String confirmPassword
    ) {
    }

    public record AdminUpdateUserRequest(
            @NotNull(message = "用户角色不能为空")
            UserRole role,

            @NotNull(message = "用户状态不能为空")
            UserStatus status
    ) {
    }
}
