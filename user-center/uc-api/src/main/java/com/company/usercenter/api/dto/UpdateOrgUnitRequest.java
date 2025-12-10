package com.company.usercenter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新组织单元请求
 */
@Schema(description = "更新组织单元请求")
public record UpdateOrgUnitRequest(
        @Schema(description = "组织名称")
        String name,
        @Schema(description = "排序序号")
        Integer sortOrder
) {}
