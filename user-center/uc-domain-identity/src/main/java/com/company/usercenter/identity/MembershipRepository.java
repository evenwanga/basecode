package com.company.usercenter.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(UUID userId);

    List<Membership> findByTenantId(UUID tenantId);

    /**
     * 检查用户是否在指定租户中有成员关系
     */
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);

    /**
     * 查找用户在指定租户中的成员关系
     */
    Optional<Membership> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    /**
     * 检查指定组织下是否存在成员
     */
    boolean existsByOrgUnitId(UUID orgUnitId);

    /**
     * 查找指定组织下的所有成员
     */
    List<Membership> findByOrgUnitId(UUID orgUnitId);
}
