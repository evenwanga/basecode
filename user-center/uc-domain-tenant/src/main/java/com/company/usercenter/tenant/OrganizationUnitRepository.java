package com.company.usercenter.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, UUID> {
    List<OrganizationUnit> findByTenantId(UUID tenantId);

    /**
     * 检查是否存在指定父组织的子组织
     */
    boolean existsByTenantIdAndParentId(UUID tenantId, UUID parentId);

    /**
     * 统计指定父组织下的子组织数量
     */
    int countByParentId(UUID parentId);
}
