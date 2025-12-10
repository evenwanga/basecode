package com.company.usercenter.tenant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    public TenantService(TenantRepository tenantRepository, OrganizationUnitRepository organizationUnitRepository) {
        this.tenantRepository = tenantRepository;
        this.organizationUnitRepository = organizationUnitRepository;
    }

    @Transactional
    public Tenant createTenant(String code, String name) {
        if (tenantRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("租户编码已存在");
        }
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(name);
        tenant.setStatus("ACTIVE");
        Tenant saved = tenantRepository.save(tenant);

        OrganizationUnit root = new OrganizationUnit();
        root.setTenantId(saved.getId());
        root.setName("Root");
        root.setStatus("ACTIVE");
        organizationUnitRepository.save(root);
        return saved;
    }

    @Transactional
    public Tenant updateTenant(UUID tenantId, String name, String status) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("租户不存在"));
        if (name != null) {
            tenant.setName(name);
        }
        if (status != null) {
            tenant.setStatus(status);
        }
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void disableTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("租户不存在"));
        tenant.setStatus("DISABLED");
        tenantRepository.save(tenant);
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> findById(UUID tenantId) {
        return tenantRepository.findById(tenantId);
    }

    public List<OrganizationUnit> listOrgUnitsForCurrentTenant() {
        String tenantIdStr = com.company.platform.jpa.TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new IllegalStateException("缺少租户标识");
        }
        UUID tenantId = UUID.fromString(tenantIdStr);
        return organizationUnitRepository.findByTenantId(tenantId);
    }

    /**
     * 查询业务租户列表（排除平台租户）
     *
     * @return 非平台类型的租户列表
     */
    public List<Tenant> listBusinessTenants() {
        return tenantRepository.findByTypeNot(Tenant.TenantType.PLATFORM);
    }
}
