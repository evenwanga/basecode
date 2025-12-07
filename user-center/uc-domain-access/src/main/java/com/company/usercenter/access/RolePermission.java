package com.company.usercenter.access;

import com.company.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "role_permissions")
public class RolePermission extends BaseEntity {

    @NotNull
    @Column(nullable = false)
    private UUID roleId;

    @NotNull
    @Column(nullable = false)
    private UUID permissionId;

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }
}
