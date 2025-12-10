package com.company.usercenter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * 组织树节点
 */
@Schema(description = "组织树节点")
public record OrgTreeNode(
        @Schema(description = "组织ID")
        UUID id,
        @Schema(description = "组织名称")
        String name,
        @Schema(description = "组织状态")
        String status,
        @Schema(description = "子组织列表")
        List<OrgTreeNode> children
) {}
