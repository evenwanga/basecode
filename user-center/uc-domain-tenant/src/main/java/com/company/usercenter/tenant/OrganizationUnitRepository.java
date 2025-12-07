package com.company.usercenter.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, UUID> {
    List<OrganizationUnit> findByTenantId(UUID tenantId);
}
