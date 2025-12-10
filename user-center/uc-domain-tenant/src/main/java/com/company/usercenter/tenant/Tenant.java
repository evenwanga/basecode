package com.company.usercenter.tenant;

import com.company.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    /**
     * 租户类型枚举
     */
    public enum TenantType {
        /** 平台租户（系统唯一） */
        PLATFORM,
        /** 内部租户（公司内部业务线） */
        INTERNAL,
        /** 客户租户（默认） */
        CUSTOMER,
        /** 合作伙伴租户 */
        PARTNER
    }

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantType type = TenantType.CUSTOMER;

    @Column(nullable = false)
    private String status = "ACTIVE";

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TenantType getType() {
        return type;
    }

    public void setType(TenantType type) {
        this.type = type;
    }
}
