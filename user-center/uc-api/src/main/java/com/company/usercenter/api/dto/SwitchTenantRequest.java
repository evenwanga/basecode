package com.company.usercenter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "切换租户请求")
public record SwitchTenantRequest(
        @Schema(description = "目标租户 ID，用户必须拥有该租户的成员身份才能切换", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID tenantId) {
}
