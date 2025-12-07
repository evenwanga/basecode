package com.company.usercenter.access;

import com.company.platform.jpa.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AccessService 的核心校验逻辑单测，确保租户隔离与重复绑定校验生效。
 */
@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    RoleRepository roleRepository;
    @Mock
    PermissionRepository permissionRepository;
    @Mock
    RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    AccessService accessService;

    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    @Test
    void createRoleShouldRejectDuplicateInTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        when(roleRepository.findByTenantIdAndCode(tenantId, "admin"))
                .thenReturn(Optional.of(new Role()));

        assertThatThrownBy(() -> accessService.createRole("admin", "管理员", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("角色编码已存在");
    }

    @Test
    void bindPermissionShouldRejectTenantMismatch() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());

        Role role = new Role();
        role.setTenantId(UUID.randomUUID()); // 不同租户
        Permission perm = new Permission();
        perm.setTenantId(tenantId);

        when(roleRepository.findById(any())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(any())).thenReturn(Optional.of(perm));

        assertThatThrownBy(() -> accessService.bindPermission(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("租户不匹配");
    }

    @Test
    void bindPermissionShouldRejectDuplicate() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());

        Role role = new Role();
        role.setTenantId(tenantId);
        Permission perm = new Permission();
        perm.setTenantId(tenantId);

        when(roleRepository.findById(any())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(any())).thenReturn(Optional.of(perm));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> accessService.bindPermission(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复绑定");
    }

    @Test
    void bindPermissionHappyPath() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());

        Role role = new Role();
        role.setTenantId(tenantId);
        Permission perm = new Permission();
        perm.setTenantId(tenantId);

        when(roleRepository.findById(any())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(any())).thenReturn(Optional.of(perm));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(any(), any())).thenReturn(false);

        accessService.bindPermission(UUID.randomUUID(), UUID.randomUUID());
        verify(rolePermissionRepository, times(1)).save(any());
    }
}
