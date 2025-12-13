package com.company.usercenter.tenant;

import com.company.platform.common.ApiResponse;
import com.company.platform.jpa.TenantContext;
import com.company.usercenter.api.dto.CreateOrgUnitRequest;
import com.company.usercenter.api.dto.OrgTreeNode;
import com.company.usercenter.api.dto.SwitchTenantRequest;
import com.company.usercenter.api.dto.UpdateOrgUnitRequest;
import com.company.usercenter.identity.IdentityService;
import com.company.usercenter.identity.UserIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "租户管理", description = "租户（Tenant）的创建、查询、切换等操作。租户是系统中的顶级隔离单元，每个租户拥有独立的组织架构和数据空间。")
public class TenantController {

        private final TenantService tenantService;
        private final IdentityService identityService;
        private final OrganizationService organizationService;
        private final PlatformTenantService platformTenantService;

        public TenantController(TenantService tenantService, IdentityService identityService,
                        OrganizationService organizationService, PlatformTenantService platformTenantService) {
                this.tenantService = tenantService;
                this.identityService = identityService;
                this.organizationService = organizationService;
                this.platformTenantService = platformTenantService;
        }

        @Operation(summary = "创建租户", description = "创建一个新的租户，系统会自动为该租户生成一个根组织单元。租户编码（code）在系统内必须唯一。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "租户创建成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "租户编码已存在")
        })
        @PostMapping
        public ApiResponse<Tenant> create(
                        @Parameter(description = "创建租户请求体", required = true) @Valid @RequestBody CreateTenantRequest request) {
                Tenant created = tenantService.createTenant(request.code(), request.name());
                return ApiResponse.created(created);
        }

        @Operation(summary = "查询租户列表", description = "获取系统中所有租户的列表。此接口无需租户上下文，可用于登录前选择租户场景。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回租户列表")
        })
        @GetMapping
        public ApiResponse<List<Tenant>> list() {
                return ApiResponse.ok(tenantService.listTenants());
        }

        @Operation(summary = "获取平台租户", description = "返回系统内置的平台租户信息。平台租户是系统唯一的管理租户，用于平台级别的管理操作。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回平台租户"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "平台租户未初始化")
        })
        @GetMapping("/platform")
        public ApiResponse<Tenant> getPlatformTenant() {
                return ApiResponse.ok(platformTenantService.getPlatformTenant());
        }

        @Operation(summary = "查询业务租户列表", description = "获取除平台租户外的所有业务租户列表。用于用户选择工作租户场景。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回业务租户列表")
        })
        @GetMapping("/business")
        public ApiResponse<List<Tenant>> listBusinessTenants() {
                return ApiResponse.ok(tenantService.listBusinessTenants());
        }

        @Operation(summary = "查询当前租户的组织单元", description = "获取当前租户下的所有组织单元（部门）列表。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回组织单元列表"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "未指定租户 ID")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @GetMapping("/org-units")
        public ApiResponse<List<OrganizationUnit>> listOrgUnits() {
                return ApiResponse.ok(tenantService.listOrgUnitsForCurrentTenant());
        }

        @Operation(summary = "切换租户", description = "切换当前用户的工作租户。系统会校验：1) 目标租户是否存在；2) 当前用户是否拥有该租户的成员身份（Membership）。校验通过后返回租户信息，客户端需自行持久化所选租户（如存入 Cookie 或 LocalStorage）。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "切换成功，返回目标租户信息"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "租户不存在"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未登录"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "用户无权访问该租户")
        })
        @PostMapping("/switch")
        public ApiResponse<Tenant> switchTenant(
                        @Parameter(description = "切换租户请求体", required = true) @Valid @RequestBody SwitchTenantRequest request) {
                Tenant tenant = tenantService.findById(request.tenantId())
                                .orElseThrow(() -> new IllegalArgumentException("租户不存在"));

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) {
                        return ApiResponse.error("未登录", org.springframework.http.HttpStatus.UNAUTHORIZED);
                }
                String principal = auth.getName();
                var user = identityService
                                .findByIdentifier(tenant.getId(), principal, UserIdentity.IdentityType.LOCAL_PASSWORD)
                                .or(() -> identityService.findByEmail(tenant.getId(), principal))
                                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

                if (!identityService.hasMembership(user.getId(), tenant.getId())) {
                        return ApiResponse.error("无权访问该租户", org.springframework.http.HttpStatus.FORBIDDEN);
                }

                return ApiResponse.ok(tenant);
        }

        // ==================== 组织管理 API ====================

        @Operation(summary = "创建组织单元", description = "在当前租户下创建一个组织单元（部门）。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "组织创建成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数校验失败或父组织不存在")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @PostMapping("/org-units")
        public ApiResponse<OrganizationUnit> createOrgUnit(
                        @Parameter(description = "创建组织请求体", required = true) @Valid @RequestBody CreateOrgUnitRequest request) {
                UUID tenantId = UUID.fromString(TenantContext.requireTenantId());
                OrganizationUnit created = organizationService.createOrgUnit(tenantId, request.parentId(),
                                request.name());
                return ApiResponse.created(created);
        }

        @Operation(summary = "更新组织单元", description = "更新指定组织单元的名称和/或排序序号。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "组织不存在")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @PutMapping("/org-units/{orgId}")
        public ApiResponse<OrganizationUnit> updateOrgUnit(
                        @Parameter(description = "组织单元ID", required = true) @PathVariable UUID orgId,
                        @Parameter(description = "更新组织请求体", required = true) @Valid @RequestBody UpdateOrgUnitRequest request) {
                OrganizationUnit updated = organizationService.updateOrgUnit(orgId, request.name(),
                                request.sortOrder());
                return ApiResponse.ok(updated);
        }

        @Operation(summary = "删除组织单元", description = "删除指定的组织单元。如果组织下存在子组织或成员，则无法删除。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "组织不存在或存在子组织/成员")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @DeleteMapping("/org-units/{orgId}")
        public ApiResponse<Void> deleteOrgUnit(
                        @Parameter(description = "组织单元ID", required = true) @PathVariable UUID orgId) {
                organizationService.deleteOrgUnit(orgId);
                return ApiResponse.ok(null);
        }

        @Operation(summary = "获取组织树", description = "获取当前租户下的完整组织树结构，按 sortOrder 排序。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @GetMapping("/org-tree")
        public ApiResponse<List<OrgTreeNode>> getOrgTree() {
                UUID tenantId = UUID.fromString(TenantContext.requireTenantId());
                return ApiResponse.ok(organizationService.getOrgTree(tenantId));
        }

        @Schema(description = "创建租户请求")
        public record CreateTenantRequest(
                        @Schema(description = "租户编码，系统唯一标识，建议使用英文小写+数字", example = "acme_corp", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String code,
                        @Schema(description = "租户名称，用于显示", example = "艾克米公司", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String name) {
        }
}
