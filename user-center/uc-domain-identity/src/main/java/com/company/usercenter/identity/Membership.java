package com.company.usercenter.identity;

import com.company.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class Membership extends BaseEntity {

    @NotNull
    @Column(nullable = false)
    private UUID userId;

    @NotNull
    @Column(nullable = false)
    private UUID tenantId;

    @Column
    private UUID orgUnitId;

    @Column
    private String roles; // 简化为逗号分隔，占位

    @Column(nullable = false)
    private String status = "ACTIVE";

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(UUID orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
