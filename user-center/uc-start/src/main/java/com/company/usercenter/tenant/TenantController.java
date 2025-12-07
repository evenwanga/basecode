package com.company.usercenter.tenant;

import com.company.platform.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /** 创建租户并生成根组织。 */
    @PostMapping
    public ApiResponse<Tenant> create(@Valid @RequestBody CreateTenantRequest request) {
        Tenant created = tenantService.createTenant(request.code(), request.name());
        return ApiResponse.created(created);
    }

    /** 租户列表。 */
    @GetMapping
    public ApiResponse<List<Tenant>> list() {
        return ApiResponse.ok(tenantService.listTenants());
    }

    /** 查看租户的组织单元。 */
    @GetMapping("/org-units")
    public ApiResponse<List<OrganizationUnit>> listOrgUnits() {
        return ApiResponse.ok(tenantService.listOrgUnitsForCurrentTenant());
    }

    public record CreateTenantRequest(@NotBlank String code, @NotBlank String name) { }
}
