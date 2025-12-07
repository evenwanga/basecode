package com.company.usercenter.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    List<RolePermission> findByRoleId(UUID roleId);
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
