package com.company.usercenter.access;

import com.company.platform.common.ApiResponse;
import com.company.platform.jpa.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 角色与权限管理接口，需在租户上下文下调用。
 */
@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final AccessService accessService;

    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    /** 创建角色（租户内 code 唯一）。 */
    @PostMapping("/roles")
    public ApiResponse<Role> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role role = accessService.createRole(request.code(), request.name(), request.description());
        return ApiResponse.created(role);
    }

    /** 创建权限（租户内 code 唯一）。 */
    @PostMapping("/permissions")
    public ApiResponse<Permission> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        Permission perm = accessService.createPermission(request.code(), request.description());
        return ApiResponse.created(perm);
    }

    /** 角色绑定权限。 */
    @PostMapping("/role-permissions")
    public ApiResponse<Void> bindPermission(@Valid @RequestBody BindRequest request) {
        accessService.bindPermission(request.roleId(), request.permissionId());
        return ApiResponse.ok(null);
    }

    /** 查看角色的权限列表。 */
    @GetMapping("/role-permissions")
    public ApiResponse<List<RolePermission>> listPermissions(@RequestParam UUID roleId) {
        List<RolePermission> list = accessService.permissionsOfRole(roleId);
        return ApiResponse.ok(list);
    }

    public record CreateRoleRequest(@NotBlank String code,
                                    @NotBlank String name,
                                    String description) { }

    public record CreatePermissionRequest(@NotBlank String code,
                                          String description) { }

    public record BindRequest(@NotNull UUID roleId, @NotNull UUID permissionId) { }
}
