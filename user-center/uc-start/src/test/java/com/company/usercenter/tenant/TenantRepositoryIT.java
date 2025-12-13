package com.company.usercenter.tenant;

import com.company.usercenter.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantRepository 集成测试")
class TenantRepositoryIT extends IntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("应能按类型查询租户")
    void findByType_shouldReturnMatchingTenants() {
        // Given: 创建不同类型的租户
        Tenant customer = createTenant("cust1_" + System.currentTimeMillis(), "客户1", Tenant.TenantType.CUSTOMER);
        Tenant partner = createTenant("partner1_" + System.currentTimeMillis(), "合作伙伴1", Tenant.TenantType.PARTNER);
        tenantRepository.saveAll(List.of(customer, partner));

        // When: 按 CUSTOMER 类型查询
        // 注意：数据库中可能已有其他数据，所以我们检查包含关系
        List<Tenant> customers = tenantRepository.findByType(Tenant.TenantType.CUSTOMER);

        // Then: 应只返回 CUSTOMER 类型
        assertThat(customers).extracting(Tenant::getCode)
                .contains(customer.getCode())
                .doesNotContain(partner.getCode());

        assertThat(customers).allMatch(t -> t.getType() == Tenant.TenantType.CUSTOMER);
    }

    @Test
    @DisplayName("应能查询非平台租户")
    void findByTypeNot_shouldExcludePlatformTenant() {
        // Given: 创建平台租户和业务租户
        // 平台租户通常只有一个且初始化时已存在，这里创建额外的测试用例
        Tenant customer = createTenant("cust2_" + System.currentTimeMillis(), "客户2", Tenant.TenantType.CUSTOMER);
        tenantRepository.save(customer);

        // When: 查询非 PLATFORM 类型
        List<Tenant> businessTenants = tenantRepository.findByTypeNot(Tenant.TenantType.PLATFORM);

        // Then: 不应包含平台租户
        assertThat(businessTenants)
                .extracting(Tenant::getType)
                .doesNotContain(Tenant.TenantType.PLATFORM);

        assertThat(businessTenants)
                .extracting(Tenant::getCode)
                .contains(customer.getCode());
    }

    private Tenant createTenant(String code, String name, Tenant.TenantType type) {
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(name);
        tenant.setType(type);
        tenant.setStatus("ACTIVE");
        return tenant;
    }
}
