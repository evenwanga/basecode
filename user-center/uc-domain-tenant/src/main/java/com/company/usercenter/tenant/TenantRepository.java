package com.company.usercenter.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByCode(String code);

    /**
     * 按租户类型查询
     */
    List<Tenant> findByType(Tenant.TenantType type);

    /**
     * 查询指定类型以外的租户（如排除平台租户）
     */
    List<Tenant> findByTypeNot(Tenant.TenantType type);

    /**
     * 查询指定类型的第一个租户
     */
    Optional<Tenant> findFirstByType(Tenant.TenantType type);
}
