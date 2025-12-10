package com.company.usercenter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * 创建组织单元请求
 */
@Schema(description = "创建组织单元请求")
public record CreateOrgUnitRequest(
        @Schema(description = "父组织ID，为空则创建顶级组织")
        UUID parentId,
        @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "研发部")
        @NotBlank(message = "组织名称不能为空")
        String name
) {}
