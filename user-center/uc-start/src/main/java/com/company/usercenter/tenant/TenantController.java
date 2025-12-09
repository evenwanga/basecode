package com.company.usercenter.tenant;

import com.company.platform.common.ApiResponse;
import com.company.usercenter.api.dto.SwitchTenantRequest;
import com.company.usercenter.identity.IdentityService;
import com.company.usercenter.identity.UserIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "租户管理", description = "租户（Tenant）的创建、查询、切换等操作。租户是系统中的顶级隔离单元，每个租户拥有独立的组织架构和数据空间。")
public class TenantController {

    private final TenantService tenantService;
    private final IdentityService identityService;

    public TenantController(TenantService tenantService, IdentityService identityService) {
        this.tenantService = tenantService;
        this.identityService = identityService;
    }

    @Operation(
            summary = "创建租户",
            description = "创建一个新的租户，系统会自动为该租户生成一个根组织单元。租户编码（code）在系统内必须唯一。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "租户创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "租户编码已存在")
    })
    @PostMapping
    public ApiResponse<Tenant> create(
            @Parameter(description = "创建租户请求体", required = true)
            @Valid @RequestBody CreateTenantRequest request) {
        Tenant created = tenantService.createTenant(request.code(), request.name());
        return ApiResponse.created(created);
    }

    @Operation(
            summary = "查询租户列表",
            description = "获取系统中所有租户的列表。此接口无需租户上下文，可用于登录前选择租户场景。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回租户列表")
    })
    @GetMapping
    public ApiResponse<List<Tenant>> list() {
        return ApiResponse.ok(tenantService.listTenants());
    }

    @Operation(
            summary = "查询当前租户的组织单元",
            description = "获取当前租户下的所有组织单元（部门）列表。需要在请求头中携带 X-Tenant-Id 指定当前租户。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回组织单元列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "未指定租户 ID")
    })
    @GetMapping("/org-units")
    public ApiResponse<List<OrganizationUnit>> listOrgUnits() {
        return ApiResponse.ok(tenantService.listOrgUnitsForCurrentTenant());
    }

    @Operation(
            summary = "切换租户",
            description = "切换当前用户的工作租户。系统会校验：1) 目标租户是否存在；2) 当前用户是否拥有该租户的成员身份（Membership）。校验通过后返回租户信息，客户端需自行持久化所选租户（如存入 Cookie 或 LocalStorage）。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "切换成功，返回目标租户信息"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "租户不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未登录"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "用户无权访问该租户")
    })
    @PostMapping("/switch")
    public ApiResponse<Tenant> switchTenant(
            @Parameter(description = "切换租户请求体", required = true)
            @Valid @RequestBody SwitchTenantRequest request) {
        Tenant tenant = tenantService.findById(request.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("租户不存在"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ApiResponse.error("未登录", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        String principal = auth.getName();
        var user = identityService.findByIdentifier(principal, UserIdentity.IdentityType.LOCAL_PASSWORD)
                .or(() -> identityService.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (!identityService.hasMembership(user.getId(), tenant.getId())) {
            return ApiResponse.error("无权访问该租户", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        return ApiResponse.ok(tenant);
    }

    @Schema(description = "创建租户请求")
    public record CreateTenantRequest(
            @Schema(description = "租户编码，系统唯一标识，建议使用英文小写+数字", example = "acme_corp", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String code,
            @Schema(description = "租户名称，用于显示", example = "艾克米公司", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String name) { }
}
