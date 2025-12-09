package com.company.usercenter.access;

import com.company.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/access")
@Tag(name = "权限管理", description = "基于角色的访问控制（RBAC）管理接口。包括角色（Role）、权限（Permission）的创建和角色-权限绑定。所有操作都在当前租户上下文中执行，需要在请求头中携带 X-Tenant-Id。")
public class AccessController {

    private final AccessService accessService;

    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    @Operation(
            summary = "创建角色",
            description = "在当前租户下创建一个新角色。角色编码（code）在租户内必须唯一，常用于程序中引用角色；角色名称（name）用于界面显示。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "角色创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败或未指定租户"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "角色编码在当前租户内已存在")
    })
    @PostMapping("/roles")
    public ApiResponse<Role> createRole(
            @Parameter(description = "创建角色请求体", required = true)
            @Valid @RequestBody CreateRoleRequest request) {
        Role role = accessService.createRole(request.code(), request.name(), request.description());
        return ApiResponse.created(role);
    }

    @Operation(
            summary = "创建权限",
            description = "在当前租户下创建一个新权限。权限编码（code）在租户内必须唯一，通常采用 \"模块:操作\" 的命名方式，如 \"user:create\"、\"order:delete\" 等。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "权限创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败或未指定租户"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "权限编码在当前租户内已存在")
    })
    @PostMapping("/permissions")
    public ApiResponse<Permission> createPermission(
            @Parameter(description = "创建权限请求体", required = true)
            @Valid @RequestBody CreatePermissionRequest request) {
        Permission perm = accessService.createPermission(request.code(), request.description());
        return ApiResponse.created(perm);
    }

    @Operation(
            summary = "为角色绑定权限",
            description = "将一个权限授予指定角色。绑定后，拥有该角色的用户将自动获得此权限。重复绑定同一权限不会产生副作用。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "绑定成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "角色或权限不存在")
    })
    @PostMapping("/role-permissions")
    public ApiResponse<Void> bindPermission(
            @Parameter(description = "角色-权限绑定请求体", required = true)
            @Valid @RequestBody BindRequest request) {
        accessService.bindPermission(request.roleId(), request.permissionId());
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "查询角色的权限列表",
            description = "获取指定角色已绑定的所有权限记录。返回的是角色-权限关联记录列表，包含绑定的创建时间等信息。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回权限列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @GetMapping("/role-permissions")
    public ApiResponse<List<RolePermission>> listPermissions(
            @Parameter(description = "角色 ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam UUID roleId) {
        List<RolePermission> list = accessService.permissionsOfRole(roleId);
        return ApiResponse.ok(list);
    }

    @Schema(description = "创建角色请求")
    public record CreateRoleRequest(
            @Schema(description = "角色编码，租户内唯一，建议使用英文小写+下划线", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String code,
            @Schema(description = "角色名称，用于显示", example = "系统管理员", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String name,
            @Schema(description = "角色描述", example = "拥有系统所有权限的超级管理员")
            String description) { }

    @Schema(description = "创建权限请求")
    public record CreatePermissionRequest(
            @Schema(description = "权限编码，租户内唯一，建议使用 \"模块:操作\" 格式", example = "user:create", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String code,
            @Schema(description = "权限描述", example = "创建用户")
            String description) { }

    @Schema(description = "角色-权限绑定请求")
    public record BindRequest(
            @Schema(description = "角色 ID", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID roleId,
            @Schema(description = "权限 ID", example = "660e8400-e29b-41d4-a716-446655440001", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID permissionId) { }
}
