package com.company.usercenter.access;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccessService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public AccessService(RoleRepository roleRepository,
                         PermissionRepository permissionRepository,
                         RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional
    public Role createRole(String code, String name, String description) {
        UUID tenantId = currentTenant();
        Optional<Role> dup = roleRepository.findByTenantIdAndCode(tenantId, code);
        if (dup.isPresent()) {
            throw new IllegalArgumentException("角色编码已存在");
        }
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        return roleRepository.save(role);
    }

    @Transactional
    public Permission createPermission(String code, String description) {
        UUID tenantId = currentTenant();
        Optional<Permission> dup = permissionRepository.findByTenantIdAndCode(tenantId, code);
        if (dup.isPresent()) {
            throw new IllegalArgumentException("权限编码已存在");
        }
        Permission perm = new Permission();
        perm.setTenantId(tenantId);
        perm.setCode(code);
        perm.setDescription(description);
        return permissionRepository.save(perm);
    }

    @Transactional
    public void bindPermission(UUID roleId, UUID permissionId) {
        UUID tenantId = currentTenant();
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        Permission perm = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("权限不存在"));
        if (!tenantId.equals(role.getTenantId()) || !tenantId.equals(perm.getTenantId())) {
            throw new IllegalArgumentException("租户不匹配，拒绝绑定");
        }
        boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
        if (exists) {
            throw new IllegalArgumentException("重复绑定权限");
        }
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rolePermissionRepository.save(rp);
    }

    public List<RolePermission> permissionsOfRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        UUID tenantId = currentTenant();
        if (!tenantId.equals(role.getTenantId())) {
            throw new IllegalArgumentException("无权查看其他租户角色");
        }
        return rolePermissionRepository.findByRoleId(roleId);
    }

    private UUID currentTenant() {
        String tenantIdStr = com.company.platform.jpa.TenantContext.requireTenantId();
        return UUID.fromString(tenantIdStr);
    }
}
