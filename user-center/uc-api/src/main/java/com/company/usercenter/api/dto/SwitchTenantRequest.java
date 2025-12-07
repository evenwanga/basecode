package com.company.usercenter.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** 切换租户请求 DTO。 */
public record SwitchTenantRequest(@NotNull UUID tenantId) {
}
