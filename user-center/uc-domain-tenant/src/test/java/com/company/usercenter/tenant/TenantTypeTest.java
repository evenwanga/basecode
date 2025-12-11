package com.company.usercenter.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tenant 类型功能测试
 * TDD 6.1: 验证租户类型枚举和默认值
 */
@DisplayName("Tenant 类型功能测试")
class TenantTypeTest {

    @Test
    @DisplayName("新建租户默认类型应为 CUSTOMER")
    void newTenant_shouldHaveDefaultType_CUSTOMER() {
        Tenant tenant = new Tenant();
        assertThat(tenant.getType()).isEqualTo(Tenant.TenantType.CUSTOMER);
    }

    @Test
    @DisplayName("租户类型应支持所有枚举值")
    void tenantType_shouldSupportAllEnumValues() {
        assertThat(Tenant.TenantType.values())
            .containsExactlyInAnyOrder(
                Tenant.TenantType.PLATFORM,
                Tenant.TenantType.INTERNAL,
                Tenant.TenantType.CUSTOMER,
                Tenant.TenantType.PARTNER
            );
    }

    @Test
    @DisplayName("设置租户类型应正确保存")
    void setType_shouldPersistCorrectly() {
        Tenant tenant = new Tenant();
        tenant.setType(Tenant.TenantType.PARTNER);
        assertThat(tenant.getType()).isEqualTo(Tenant.TenantType.PARTNER);
    }

    @Test
    @DisplayName("PLATFORM 类型应能正确设置")
    void setType_PLATFORM_shouldWork() {
        Tenant tenant = new Tenant();
        tenant.setType(Tenant.TenantType.PLATFORM);
        assertThat(tenant.getType()).isEqualTo(Tenant.TenantType.PLATFORM);
    }

    @Test
    @DisplayName("INTERNAL 类型应能正确设置")
    void setType_INTERNAL_shouldWork() {
        Tenant tenant = new Tenant();
        tenant.setType(Tenant.TenantType.INTERNAL);
        assertThat(tenant.getType()).isEqualTo(Tenant.TenantType.INTERNAL);
    }
}



