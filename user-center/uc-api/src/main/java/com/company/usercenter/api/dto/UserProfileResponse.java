package com.company.usercenter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "用户个人资料响应")
public record UserProfileResponse(
        @Schema(description = "用户唯一标识", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "用户显示名称", example = "张三")
        String displayName,
        @Schema(description = "用户主邮箱", example = "zhangsan@example.com")
        String primaryEmail,
        @Schema(description = "用户状态：ACTIVE-正常，SUSPENDED-已停用，PENDING-待激活", example = "ACTIVE")
        String status
) {
}
