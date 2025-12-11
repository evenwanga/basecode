# 用户中心领域增强开发计划

> 本文档基于架构讨论整理，作为 `plan.md` 的补充，聚焦于**领域模型完善**和**平台级组织架构**支持。
> 
> ⚠️ **开发模式**：本计划采用 **TDD（测试驱动开发）** 模式，遵循 Red → Green → Refactor 循环。

---

## 〇、TDD 开发流程规范

### 0.1 TDD 三步循环

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          TDD 开发循环                                        │
│                                                                             │
│   ┌─────────┐         ┌─────────┐         ┌─────────┐                      │
│   │  RED    │────────►│  GREEN  │────────►│REFACTOR │                      │
│   │ 写失败  │         │ 写最少  │         │  重构   │                      │
│   │ 的测试  │         │ 的代码  │         │  优化   │                      │
│   └─────────┘         └─────────┘         └────┬────┘                      │
│        ▲                                       │                           │
│        └───────────────────────────────────────┘                           │
│                     下一个功能点                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 0.2 每个任务的标准执行步骤

1. **📝 定义验收标准** - 明确功能需求和边界条件
2. **🔴 编写测试用例** - 先写测试，测试应该失败（Red）
3. **🟢 实现最小代码** - 写最少的代码让测试通过（Green）
4. **🔵 重构优化** - 保持测试通过的前提下优化代码（Refactor）
5. **✅ 验收确认** - 确保所有测试通过，功能符合预期

### 0.3 测试分层策略

| 层级 | 类型 | 命名规范 | 说明 |
|------|------|---------|------|
| 1 | 领域单元测试 | `*Test.java` | 不启动 Spring，纯 Java 测试 |
| 2 | 服务集成测试 | `*ServiceIT.java` | 启动精简上下文，使用 Testcontainers |
| 3 | API 端到端测试 | `*ControllerIT.java` | 完整 HTTP 请求测试 |

---

## 一、当前领域模型总览

### 1.1 核心实体关系图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              OAuth2 认证层                                       │
│  ┌───────────────────────┐          ┌───────────────────────┐                   │
│  │ oauth2_registered_    │          │ oauth2_authorization  │                   │
│  │ client (应用客户端)   │◄────────►│ (授权/令牌存储)        │                   │
│  └───────────────────────┘          └───────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              业务领域层                                          │
│                                                                                 │
│  ┌──────────────┐     1:N      ┌──────────────────┐                             │
│  │   Tenant     │◄────────────►│ OrganizationUnit │                             │
│  │  (租户)      │              │   (组织单元)      │                             │
│  │  - type ⭐新  │              │  - parentId      │ ← 支持无限级树形结构         │
│  └──────┬───────┘              └──────────────────┘                             │
│         │                                                                       │
│         │ 1:N (租户级隔离)                                                       │
│         ▼                                                                       │
│  ┌──────────────┐     N:M      ┌──────────────┐                                 │
│  │    Role      │◄────────────►│ Permission   │                                 │
│  │   (角色)     │              │   (权限)     │                                 │
│  └──────────────┘              └──────────────┘                                 │
│                                                                                 │
│  ┌──────────────┐     1:N      ┌──────────────────┐                             │
│  │    User      │◄────────────►│  UserIdentity    │                             │
│  │  (全局用户)   │              │   (身份标识)      │                             │
│  └──────┬───────┘              │  - LOCAL_PASSWORD│                             │
│         │                      │  - EMAIL_OTP     │                             │
│         │                      │  - PHONE_OTP     │                             │
│         │                      │  - EXTERNAL_OIDC │                             │
│         │                      └──────────────────┘                             │
│         │ 1:N                                                                    │
│         ▼                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │                    Membership (成员关系)                       │               │
│  │   - userId    → 关联 User                                     │               │
│  │   - tenantId  → 关联 Tenant                                   │               │
│  │   - orgUnitId → 关联 OrganizationUnit (可选)                   │               │
│  │   - roles     → 角色列表 (当前为逗号分隔字符串)                  │               │
│  └──────────────────────────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计理念

| 设计点 | 说明 |
|--------|------|
| **User 全局化** | 用户不绑定租户，支持一个人加入多个租户 |
| **Membership 桥接** | 通过成员关系表连接 User 和 Tenant，并存储角色 |
| **租户级 RBAC** | Role 和 Permission 都带 `tenantId`，实现租户隔离 |
| **多身份支持** | UserIdentity 支持密码、OTP、外部 OIDC 等多种登录方式 |
| **组织树形结构** | OrganizationUnit 通过 `parentId` 支持无限级嵌套 |

---

## 二、待补充功能清单

### 2.1 功能矩阵

| 模块 | 功能 | 当前状态 | 优先级 | 预估工时 |
|------|------|---------|--------|----------|
| **Tenant** | 租户类型字段 (type) | ❌ 未实现 | P0 | 2h |
| **Tenant** | 平台租户初始化 | ❌ 未实现 | P0 | 2h |
| **Tenant** | 平台租户服务 | ❌ 未实现 | P0 | 4h |
| **OrgUnit** | 创建子组织 API | ❌ 未实现 | P1 | 3h |
| **OrgUnit** | 更新组织 API | ❌ 未实现 | P1 | 2h |
| **OrgUnit** | 删除组织 API | ❌ 未实现 | P1 | 3h |
| **OrgUnit** | 移动组织 API | ❌ 未实现 | P2 | 4h |
| **OrgUnit** | path/level 字段 | ❌ 未实现 | P2 | 4h |
| **Membership** | roles 改为关联表 | ❌ 未实现 | P2 | 6h |
| **Test** | 组织管理单元测试 | ❌ 未实现 | P1 | 3h |
| **Test** | 平台租户集成测试 | ❌ 未实现 | P1 | 3h |

---

## 三、TDD 开发计划详情

### 第六阶段：平台租户与组织架构增强

> 预估总工时：**36h (约 4-5 个工作日)**
> 
> 🎯 **TDD 原则**：每个任务先写测试，再写实现

---

#### 任务 6.1：Tenant 类型支持 [P0] [4h]

**目标**：为 Tenant 添加类型字段，支持区分平台租户和业务租户

##### 📝 Step 1: 定义验收标准

| # | 验收条件 | 测试类型 |
|---|---------|---------|
| 1 | Tenant 实体包含 type 字段，默认值为 CUSTOMER | 单元测试 |
| 2 | type 字段支持 PLATFORM/INTERNAL/CUSTOMER/PARTNER 四种枚举值 | 单元测试 |
| 3 | 可按 type 查询租户列表 | 集成测试 |
| 4 | 数据库迁移后平台租户自动初始化 | 集成测试 |
| 5 | 创建新租户时默认 type=CUSTOMER | 单元测试 |

##### 🔴 Step 2: 编写测试用例（先写，应该失败）

**文件**: `uc-domain-tenant/src/test/java/com/company/usercenter/tenant/TenantTypeTest.java`

```java
package com.company.usercenter.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

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
}
```

**文件**: `uc-domain-tenant/src/test/java/com/company/usercenter/tenant/TenantRepositoryIT.java`

```java
package com.company.usercenter.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@DisplayName("TenantRepository 集成测试")
class TenantRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("应能按类型查询租户")
    void findByType_shouldReturnMatchingTenants() {
        // Given: 创建不同类型的租户
        Tenant customer = createTenant("cust1", "客户1", Tenant.TenantType.CUSTOMER);
        Tenant partner = createTenant("partner1", "合作伙伴1", Tenant.TenantType.PARTNER);
        tenantRepository.saveAll(List.of(customer, partner));

        // When: 按 CUSTOMER 类型查询
        List<Tenant> customers = tenantRepository.findByType(Tenant.TenantType.CUSTOMER);

        // Then: 应只返回 CUSTOMER 类型
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getCode()).isEqualTo("cust1");
    }

    @Test
    @DisplayName("应能查询非平台租户")
    void findByTypeNot_shouldExcludePlatformTenant() {
        // Given: 创建不同类型的租户
        Tenant platform = createTenant("__platform__", "平台", Tenant.TenantType.PLATFORM);
        Tenant customer = createTenant("cust1", "客户1", Tenant.TenantType.CUSTOMER);
        tenantRepository.saveAll(List.of(platform, customer));

        // When: 查询非 PLATFORM 类型
        List<Tenant> businessTenants = tenantRepository.findByTypeNot(Tenant.TenantType.PLATFORM);

        // Then: 不应包含平台租户
        assertThat(businessTenants).hasSize(1);
        assertThat(businessTenants).noneMatch(t -> t.getType() == Tenant.TenantType.PLATFORM);
    }

    private Tenant createTenant(String code, String name, Tenant.TenantType type) {
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(name);
        tenant.setType(type);
        return tenant;
    }
}
```

##### 🟢 Step 3: 实现代码（让测试通过）

**3.1 数据库迁移** - 创建 `V5__tenant_type_and_platform.sql`：

```sql
-- 1. 添加租户类型字段
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS type VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER';
CREATE INDEX IF NOT EXISTS idx_tenants_type ON tenants(type);

-- 2. 插入平台租户（系统内置）
INSERT INTO tenants (id, code, name, type, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '__platform__',
    '平台',
    'PLATFORM',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

-- 3. 为平台租户创建根组织
INSERT INTO organization_units (id, tenant_id, parent_id, name, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    NULL,
    'Root',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;
```

**3.2 修改 Tenant 实体**：

```java
// Tenant.java - 添加枚举和字段
public enum TenantType {
    PLATFORM,   // 平台租户（系统唯一）
    INTERNAL,   // 内部租户（公司内部业务线）
    CUSTOMER,   // 客户租户（默认）
    PARTNER     // 合作伙伴租户
}

@Enumerated(EnumType.STRING)
@Column(nullable = false)
private TenantType type = TenantType.CUSTOMER;

public TenantType getType() { return type; }
public void setType(TenantType type) { this.type = type; }
```

**3.3 扩展 TenantRepository**：

```java
// TenantRepository.java - 添加查询方法
List<Tenant> findByType(TenantType type);
List<Tenant> findByTypeNot(TenantType type);
Optional<Tenant> findFirstByType(TenantType type);
```

##### 🔵 Step 4: 重构优化

- 确保代码风格一致
- 检查是否有重复逻辑可提取
- 优化测试的可读性

##### ✅ Step 5: 验收检查

```bash
# 运行测试
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantRepositoryIT

# 验证迁移
mvn flyway:migrate -pl user-center/uc-start
```

**验收清单**：
- [ ] `TenantTypeTest` 全部通过
- [ ] `TenantRepositoryIT` 全部通过
- [ ] 数据库迁移成功执行
- [ ] 平台租户已初始化

---

#### 任务 6.2：平台租户服务 [P0] [4h]

**目标**：提供平台租户相关的服务能力

##### 📝 Step 1: 定义验收标准

| # | 验收条件 | 测试类型 |
|---|---------|---------|
| 1 | 能获取平台租户实例 | 单元测试 |
| 2 | 能判断某租户是否为平台租户 | 单元测试 |
| 3 | 能判断用户是否为平台成员 | 单元测试 |
| 4 | 能判断用户是否为平台管理员 | 单元测试 |
| 5 | 能将用户添加为平台成员 | 单元测试 |
| 6 | 重复添加平台成员应抛异常 | 单元测试 |
| 7 | API `/api/tenants/business` 返回非平台租户 | API测试 |
| 8 | API `/api/tenants/platform` 返回平台租户 | API测试 |

##### 🔴 Step 2: 编写测试用例（先写，应该失败）

**文件**: `uc-domain-tenant/src/test/java/com/company/usercenter/tenant/PlatformTenantServiceTest.java`

```java
package com.company.usercenter.tenant;

import com.company.usercenter.identity.Membership;
import com.company.usercenter.identity.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformTenantService 单元测试")
class PlatformTenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private MembershipRepository membershipRepository;

    private PlatformTenantService service;

    @BeforeEach
    void setUp() {
        service = new PlatformTenantService(tenantRepository, membershipRepository);
    }

    @Nested
    @DisplayName("getPlatformTenant")
    class GetPlatformTenant {

        @Test
        @DisplayName("平台租户存在时应返回租户实例")
        void shouldReturnPlatformTenant_whenExists() {
            // Given
            Tenant platformTenant = new Tenant();
            platformTenant.setCode("__platform__");
            platformTenant.setType(Tenant.TenantType.PLATFORM);
            when(tenantRepository.findByCode("__platform__")).thenReturn(Optional.of(platformTenant));

            // When
            Tenant result = service.getPlatformTenant();

            // Then
            assertThat(result.getCode()).isEqualTo("__platform__");
        }

        @Test
        @DisplayName("平台租户不存在时应抛出异常")
        void shouldThrowException_whenNotExists() {
            // Given
            when(tenantRepository.findByCode("__platform__")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getPlatformTenant())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("平台租户未初始化");
        }
    }

    @Nested
    @DisplayName("isPlatformTenant")
    class IsPlatformTenant {

        @Test
        @DisplayName("平台租户ID应返回true")
        void shouldReturnTrue_forPlatformTenantId() {
            UUID platformId = PlatformTenantService.PLATFORM_TENANT_ID;
            assertThat(service.isPlatformTenant(platformId)).isTrue();
        }

        @Test
        @DisplayName("非平台租户ID应返回false")
        void shouldReturnFalse_forOtherTenantId() {
            UUID otherId = UUID.randomUUID();
            assertThat(service.isPlatformTenant(otherId)).isFalse();
        }
    }

    @Nested
    @DisplayName("isPlatformMember")
    class IsPlatformMember {

        @Test
        @DisplayName("用户是平台成员时应返回true")
        void shouldReturnTrue_whenUserIsPlatformMember() {
            // Given
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(true);

            // When & Then
            assertThat(service.isPlatformMember(userId)).isTrue();
        }

        @Test
        @DisplayName("用户不是平台成员时应返回false")
        void shouldReturnFalse_whenUserIsNotPlatformMember() {
            // Given
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(false);

            // When & Then
            assertThat(service.isPlatformMember(userId)).isFalse();
        }
    }

    @Nested
    @DisplayName("isPlatformAdmin")
    class IsPlatformAdmin {

        @Test
        @DisplayName("用户有PLATFORM_ADMIN角色时应返回true")
        void shouldReturnTrue_whenUserHasPlatformAdminRole() {
            // Given
            UUID userId = UUID.randomUUID();
            Membership membership = new Membership();
            membership.setRoles("PLATFORM_ADMIN,USER");
            when(membershipRepository.findByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(Optional.of(membership));

            // When & Then
            assertThat(service.isPlatformAdmin(userId)).isTrue();
        }

        @Test
        @DisplayName("用户无PLATFORM_ADMIN角色时应返回false")
        void shouldReturnFalse_whenUserLacksPlatformAdminRole() {
            // Given
            UUID userId = UUID.randomUUID();
            Membership membership = new Membership();
            membership.setRoles("USER");
            when(membershipRepository.findByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(Optional.of(membership));

            // When & Then
            assertThat(service.isPlatformAdmin(userId)).isFalse();
        }
    }

    @Nested
    @DisplayName("addPlatformMember")
    class AddPlatformMember {

        @Test
        @DisplayName("新用户应成功添加为平台成员")
        void shouldAddNewMember_successfully() {
            // Given
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(false);
            when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Membership result = service.addPlatformMember(userId, "USER");

            // Then
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getTenantId()).isEqualTo(PlatformTenantService.PLATFORM_TENANT_ID);
            assertThat(result.getRoles()).isEqualTo("USER");
        }

        @Test
        @DisplayName("已存在的平台成员应抛出异常")
        void shouldThrowException_whenAlreadyPlatformMember() {
            // Given
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.addPlatformMember(userId, "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户已是平台成员");
        }
    }
}
```

**文件**: `uc-start/src/test/java/com/company/usercenter/tenant/TenantControllerPlatformIT.java`

```java
package com.company.usercenter.tenant;

import com.company.usercenter.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("TenantController 平台租户 API 测试")
class TenantControllerPlatformIT extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/tenants/platform 应返回平台租户")
    void getPlatformTenant_shouldReturnPlatformTenant() throws Exception {
        mockMvc.perform(get("/api/tenants/platform"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.code").value("__platform__"))
            .andExpect(jsonPath("$.data.type").value("PLATFORM"));
    }

    @Test
    @DisplayName("GET /api/tenants/business 应返回非平台租户列表")
    void listBusinessTenants_shouldExcludePlatformTenant() throws Exception {
        mockMvc.perform(get("/api/tenants/business"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.type=='PLATFORM')]").doesNotExist());
    }
}
```

##### 🟢 Step 3: 实现代码（让测试通过）

**3.1 创建 PlatformTenantService**：

```java
package com.company.usercenter.tenant;

import com.company.usercenter.identity.Membership;
import com.company.usercenter.identity.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlatformTenantService {

    public static final String PLATFORM_TENANT_CODE = "__platform__";
    public static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TenantRepository tenantRepository;
    private final MembershipRepository membershipRepository;

    public PlatformTenantService(TenantRepository tenantRepository, MembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    public Tenant getPlatformTenant() {
        return tenantRepository.findByCode(PLATFORM_TENANT_CODE)
                .orElseThrow(() -> new IllegalStateException("平台租户未初始化"));
    }

    public boolean isPlatformTenant(UUID tenantId) {
        return PLATFORM_TENANT_ID.equals(tenantId);
    }

    public boolean isPlatformMember(UUID userId) {
        return membershipRepository.existsByUserIdAndTenantId(userId, PLATFORM_TENANT_ID);
    }

    public boolean isPlatformAdmin(UUID userId) {
        return membershipRepository.findByUserIdAndTenantId(userId, PLATFORM_TENANT_ID)
                .map(m -> m.getRoles() != null && m.getRoles().contains("PLATFORM_ADMIN"))
                .orElse(false);
    }

    @Transactional
    public Membership addPlatformMember(UUID userId, String roles) {
        if (membershipRepository.existsByUserIdAndTenantId(userId, PLATFORM_TENANT_ID)) {
            throw new IllegalArgumentException("用户已是平台成员");
        }
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setTenantId(PLATFORM_TENANT_ID);
        membership.setRoles(roles);
        membership.setStatus("ACTIVE");
        return membershipRepository.save(membership);
    }
}
```

**3.2 扩展 TenantService**：

```java
// TenantService.java - 添加方法
public List<Tenant> listBusinessTenants() {
    return tenantRepository.findByTypeNot(Tenant.TenantType.PLATFORM);
}
```

**3.3 扩展 TenantController**：

```java
// TenantController.java - 添加端点
@Autowired
private PlatformTenantService platformTenantService;

@GetMapping("/business")
@Operation(summary = "查询业务租户列表", description = "返回除平台租户外的所有租户，用于用户选择租户")
public ApiResponse<List<Tenant>> listBusinessTenants() {
    return ApiResponse.ok(tenantService.listBusinessTenants());
}

@GetMapping("/platform")
@Operation(summary = "获取平台租户", description = "返回平台租户信息，仅限平台管理员访问")
public ApiResponse<Tenant> getPlatformTenant() {
    return ApiResponse.ok(platformTenantService.getPlatformTenant());
}
```

**3.4 扩展 MembershipRepository**：

```java
// MembershipRepository.java - 添加方法
boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
Optional<Membership> findByUserIdAndTenantId(UUID userId, UUID tenantId);
```

##### ✅ Step 5: 验收检查

```bash
# 运行单元测试
mvn test -pl user-center/uc-domain-tenant -Dtest=PlatformTenantServiceTest

# 运行 API 测试
mvn test -pl user-center/uc-start -Dtest=TenantControllerPlatformIT
```

**验收清单**：
- [ ] `PlatformTenantServiceTest` 全部通过
- [ ] `TenantControllerPlatformIT` 全部通过
- [ ] Swagger 文档中显示新端点

---

#### 任务 6.3：组织管理 API [P1] [12h]

**目标**：完善组织单元的增删改查功能

##### 📝 Step 1: 定义验收标准

| # | 验收条件 | 测试类型 |
|---|---------|---------|
| 1 | 能创建顶级组织（parentId=null） | 单元测试 |
| 2 | 能创建子组织（指定parentId） | 单元测试 |
| 3 | 创建子组织时父组织必须存在 | 单元测试 |
| 4 | 创建子组织时父组织必须属于同一租户 | 单元测试 |
| 5 | 能更新组织名称和排序 | 单元测试 |
| 6 | 能删除无子组织且无成员的组织 | 单元测试 |
| 7 | 有子组织时禁止删除 | 单元测试 |
| 8 | 有成员时禁止删除 | 单元测试 |
| 9 | 能获取组织树结构 | 单元测试 |
| 10 | 组织树按 sortOrder 排序 | 单元测试 |
| 11 | POST `/api/tenants/org-units` 创建组织 | API测试 |
| 12 | PUT `/api/tenants/org-units/{id}` 更新组织 | API测试 |
| 13 | DELETE `/api/tenants/org-units/{id}` 删除组织 | API测试 |
| 14 | GET `/api/tenants/org-tree` 获取组织树 | API测试 |

##### 🔴 Step 2: 编写测试用例（先写，应该失败）

**文件**: `uc-domain-tenant/src/test/java/com/company/usercenter/tenant/OrganizationServiceTest.java`

```java
package com.company.usercenter.tenant;

import com.company.usercenter.identity.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService 单元测试")
class OrganizationServiceTest {

    @Mock
    private OrganizationUnitRepository orgRepository;
    @Mock
    private MembershipRepository membershipRepository;

    private OrganizationService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service = new OrganizationService(orgRepository, membershipRepository);
        tenantId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createOrgUnit - 创建组织")
    class CreateOrgUnit {

        @Test
        @DisplayName("应能创建顶级组织")
        void shouldCreateTopLevelOrg() {
            // Given
            when(orgRepository.countByParentId(null)).thenReturn(0);
            when(orgRepository.save(any())).thenAnswer(inv -> {
                OrganizationUnit org = inv.getArgument(0);
                org.setId(UUID.randomUUID());
                return org;
            });

            // When
            OrganizationUnit result = service.createOrgUnit(tenantId, null, "研发部");

            // Then
            assertThat(result.getName()).isEqualTo("研发部");
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("应能创建子组织")
        void shouldCreateChildOrg() {
            // Given
            UUID parentId = UUID.randomUUID();
            OrganizationUnit parent = createOrg(parentId, tenantId, null, "根组织");
            when(orgRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(orgRepository.countByParentId(parentId)).thenReturn(2);
            when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            OrganizationUnit result = service.createOrgUnit(tenantId, parentId, "前端组");

            // Then
            assertThat(result.getParentId()).isEqualTo(parentId);
            assertThat(result.getSortOrder()).isEqualTo(2); // 已有2个子组织
        }

        @Test
        @DisplayName("父组织不存在时应抛异常")
        void shouldThrowWhenParentNotExists() {
            // Given
            UUID parentId = UUID.randomUUID();
            when(orgRepository.findById(parentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.createOrgUnit(tenantId, parentId, "子组织"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("父组织不存在");
        }

        @Test
        @DisplayName("父组织属于其他租户时应抛异常")
        void shouldThrowWhenParentBelongsToOtherTenant() {
            // Given
            UUID parentId = UUID.randomUUID();
            UUID otherTenantId = UUID.randomUUID();
            OrganizationUnit parent = createOrg(parentId, otherTenantId, null, "其他租户组织");
            when(orgRepository.findById(parentId)).thenReturn(Optional.of(parent));

            // When & Then
            assertThatThrownBy(() -> service.createOrgUnit(tenantId, parentId, "子组织"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于当前租户");
        }
    }

    @Nested
    @DisplayName("updateOrgUnit - 更新组织")
    class UpdateOrgUnit {

        @Test
        @DisplayName("应能更新组织名称")
        void shouldUpdateOrgName() {
            // Given
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "旧名称");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            OrganizationUnit result = service.updateOrgUnit(orgId, "新名称", null);

            // Then
            assertThat(result.getName()).isEqualTo("新名称");
        }

        @Test
        @DisplayName("应能更新排序序号")
        void shouldUpdateSortOrder() {
            // Given
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "组织");
            org.setSortOrder(0);
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            OrganizationUnit result = service.updateOrgUnit(orgId, null, 5);

            // Then
            assertThat(result.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("组织不存在时应抛异常")
        void shouldThrowWhenOrgNotExists() {
            // Given
            UUID orgId = UUID.randomUUID();
            when(orgRepository.findById(orgId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateOrgUnit(orgId, "名称", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("组织不存在");
        }
    }

    @Nested
    @DisplayName("deleteOrgUnit - 删除组织")
    class DeleteOrgUnit {

        @Test
        @DisplayName("无子组织且无成员时应成功删除")
        void shouldDeleteWhenNoChildrenAndNoMembers() {
            // Given
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "待删除");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.existsByTenantIdAndParentId(tenantId, orgId)).thenReturn(false);
            when(membershipRepository.existsByOrgUnitId(orgId)).thenReturn(false);

            // When
            service.deleteOrgUnit(orgId);

            // Then
            verify(orgRepository).delete(org);
        }

        @Test
        @DisplayName("有子组织时应拒绝删除")
        void shouldRejectWhenHasChildren() {
            // Given
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "有子组织");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.existsByTenantIdAndParentId(tenantId, orgId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.deleteOrgUnit(orgId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("存在子组织");
        }

        @Test
        @DisplayName("有成员时应拒绝删除")
        void shouldRejectWhenHasMembers() {
            // Given
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "有成员");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.existsByTenantIdAndParentId(tenantId, orgId)).thenReturn(false);
            when(membershipRepository.existsByOrgUnitId(orgId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.deleteOrgUnit(orgId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("存在成员");
        }
    }

    @Nested
    @DisplayName("getOrgTree - 获取组织树")
    class GetOrgTree {

        @Test
        @DisplayName("应返回正确的树形结构")
        void shouldReturnCorrectTreeStructure() {
            // Given: 构建测试数据
            //   Root (sortOrder=0)
            //   ├── 研发部 (sortOrder=1)
            //   │   ├── 前端组 (sortOrder=0)
            //   │   └── 后端组 (sortOrder=1)
            //   └── 市场部 (sortOrder=0)
            UUID rootId = UUID.randomUUID();
            UUID devId = UUID.randomUUID();
            UUID marketId = UUID.randomUUID();
            UUID frontendId = UUID.randomUUID();
            UUID backendId = UUID.randomUUID();

            List<OrganizationUnit> allOrgs = List.of(
                createOrgWithSort(rootId, tenantId, null, "Root", 0),
                createOrgWithSort(devId, tenantId, rootId, "研发部", 1),
                createOrgWithSort(marketId, tenantId, rootId, "市场部", 0),
                createOrgWithSort(frontendId, tenantId, devId, "前端组", 0),
                createOrgWithSort(backendId, tenantId, devId, "后端组", 1)
            );
            when(orgRepository.findByTenantId(tenantId)).thenReturn(allOrgs);

            // When
            List<OrgTreeNode> tree = service.getOrgTree(tenantId);

            // Then
            assertThat(tree).hasSize(1); // 只有一个根节点
            OrgTreeNode root = tree.get(0);
            assertThat(root.name()).isEqualTo("Root");
            assertThat(root.children()).hasSize(2);
            // 验证子节点按 sortOrder 排序
            assertThat(root.children().get(0).name()).isEqualTo("市场部"); // sortOrder=0
            assertThat(root.children().get(1).name()).isEqualTo("研发部"); // sortOrder=1
            // 验证研发部的子组织
            OrgTreeNode dev = root.children().get(1);
            assertThat(dev.children()).hasSize(2);
            assertThat(dev.children().get(0).name()).isEqualTo("前端组");
            assertThat(dev.children().get(1).name()).isEqualTo("后端组");
        }

        @Test
        @DisplayName("空租户应返回空列表")
        void shouldReturnEmptyListForEmptyTenant() {
            // Given
            when(orgRepository.findByTenantId(tenantId)).thenReturn(Collections.emptyList());

            // When
            List<OrgTreeNode> tree = service.getOrgTree(tenantId);

            // Then
            assertThat(tree).isEmpty();
        }
    }

    // 辅助方法
    private OrganizationUnit createOrg(UUID id, UUID tenantId, UUID parentId, String name) {
        OrganizationUnit org = new OrganizationUnit();
        org.setId(id);
        org.setTenantId(tenantId);
        org.setParentId(parentId);
        org.setName(name);
        org.setStatus("ACTIVE");
        org.setSortOrder(0);
        return org;
    }

    private OrganizationUnit createOrgWithSort(UUID id, UUID tenantId, UUID parentId, String name, int sortOrder) {
        OrganizationUnit org = createOrg(id, tenantId, parentId, name);
        org.setSortOrder(sortOrder);
        return org;
    }
}
```

**文件**: `uc-start/src/test/java/com/company/usercenter/tenant/OrgUnitControllerIT.java`

```java
package com.company.usercenter.tenant;

import com.company.usercenter.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("组织管理 API 集成测试")
class OrgUnitControllerIT extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // 创建测试租户
        Tenant tenant = new Tenant();
        tenant.setCode("test-tenant-" + UUID.randomUUID().toString().substring(0, 8));
        tenant.setName("测试租户");
        tenant.setType(Tenant.TenantType.CUSTOMER);
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();
    }

    @Test
    @DisplayName("POST /api/tenants/org-units 应创建组织")
    void createOrgUnit_shouldCreateNewOrg() throws Exception {
        String requestBody = """
            {
                "name": "研发部"
            }
            """;

        mockMvc.perform(post("/api/tenants/org-units")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("研发部"))
            .andExpect(jsonPath("$.data.tenantId").value(tenantId.toString()));
    }

    @Test
    @DisplayName("POST /api/tenants/org-units 应创建子组织")
    void createOrgUnit_shouldCreateChildOrg() throws Exception {
        // 先创建父组织
        String parentRequest = """
            {
                "name": "研发部"
            }
            """;
        MvcResult parentResult = mockMvc.perform(post("/api/tenants/org-units")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(parentRequest))
            .andReturn();
        
        String parentId = objectMapper.readTree(parentResult.getResponse().getContentAsString())
            .path("data").path("id").asText();

        // 创建子组织
        String childRequest = String.format("""
            {
                "parentId": "%s",
                "name": "前端组"
            }
            """, parentId);

        mockMvc.perform(post("/api/tenants/org-units")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(childRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("前端组"))
            .andExpect(jsonPath("$.data.parentId").value(parentId));
    }

    @Test
    @DisplayName("PUT /api/tenants/org-units/{id} 应更新组织")
    void updateOrgUnit_shouldUpdateOrg() throws Exception {
        // 先创建组织
        String createRequest = """
            {
                "name": "旧名称"
            }
            """;
        MvcResult createResult = mockMvc.perform(post("/api/tenants/org-units")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
            .andReturn();
        
        String orgId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .path("data").path("id").asText();

        // 更新组织
        String updateRequest = """
            {
                "name": "新名称",
                "sortOrder": 5
            }
            """;

        mockMvc.perform(put("/api/tenants/org-units/" + orgId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("新名称"))
            .andExpect(jsonPath("$.data.sortOrder").value(5));
    }

    @Test
    @DisplayName("DELETE /api/tenants/org-units/{id} 应删除组织")
    void deleteOrgUnit_shouldDeleteOrg() throws Exception {
        // 先创建组织
        String createRequest = """
            {
                "name": "待删除组织"
            }
            """;
        MvcResult createResult = mockMvc.perform(post("/api/tenants/org-units")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
            .andReturn();
        
        String orgId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .path("data").path("id").asText();

        // 删除组织
        mockMvc.perform(delete("/api/tenants/org-units/" + orgId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/tenants/org-tree 应返回组织树")
    void getOrgTree_shouldReturnTree() throws Exception {
        // 创建组织结构
        String rootRequest = """{"name": "Root"}""";
        MvcResult rootResult = mockMvc.perform(post("/api/tenants/org-units")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(rootRequest))
            .andReturn();
        String rootId = objectMapper.readTree(rootResult.getResponse().getContentAsString())
            .path("data").path("id").asText();

        String childRequest = String.format("""{"parentId": "%s", "name": "子部门"}""", rootId);
        mockMvc.perform(post("/api/tenants/org-units")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(childRequest));

        // 获取组织树
        mockMvc.perform(get("/api/tenants/org-tree")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].name").value("Root"))
            .andExpect(jsonPath("$.data[0].children[0].name").value("子部门"));
    }
}
```

##### 🟢 Step 3: 实现代码（让测试通过）

**3.1 添加 sortOrder 字段到 OrganizationUnit**：

```java
// OrganizationUnit.java - 添加字段
@Column
private Integer sortOrder = 0;

public Integer getSortOrder() { return sortOrder; }
public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
```

**3.2 数据库迁移 V6__org_sort_order.sql**：

```sql
ALTER TABLE organization_units ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
```

**3.3 扩展 OrganizationUnitRepository**：

```java
boolean existsByTenantIdAndParentId(UUID tenantId, UUID parentId);
int countByParentId(UUID parentId);
```

**3.4 创建 OrganizationService** (按 Step 2 测试的实现)

**3.5 创建 DTO**：

```java
// OrgTreeNode.java
public record OrgTreeNode(UUID id, String name, String status, List<OrgTreeNode> children) {}

// CreateOrgUnitRequest.java
public record CreateOrgUnitRequest(UUID parentId, @NotBlank String name) {}

// UpdateOrgUnitRequest.java
public record UpdateOrgUnitRequest(String name, Integer sortOrder) {}
```

**3.6 扩展 TenantController**

##### ✅ Step 5: 验收检查

```bash
# 运行单元测试
mvn test -pl user-center/uc-domain-tenant -Dtest=OrganizationServiceTest

# 运行 API 测试
mvn test -pl user-center/uc-start -Dtest=OrgUnitControllerIT
```

**验收清单**：
- [ ] `OrganizationServiceTest` 全部通过 (10+ 测试用例)
- [ ] `OrgUnitControllerIT` 全部通过 (5+ 测试用例)
- [ ] Swagger 文档显示所有新端点
- [ ] 组织树结构正确返回

---

#### 任务 6.4：MembershipRepository 扩展 [P1] [2h]

**目标**：支持组织相关的成员查询

```java
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    
    // 现有方法...
    
    // 新增：按组织查询
    List<Membership> findByOrgUnitId(UUID orgUnitId);
    boolean existsByOrgUnitId(UUID orgUnitId);
    
    // 新增：按租户和用户查询
    Optional<Membership> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
}
```

---

#### 任务 6.5：测试完善 [P1] [6h]

**6.5.1 单元测试**

- `TenantServiceTest` - 租户类型测试
- `PlatformTenantServiceTest` - 平台租户服务测试
- `OrganizationServiceTest` - 组织管理测试

**6.5.2 集成测试**

- `TenantControllerIT` - 组织管理 API 测试
- `PlatformTenantIT` - 平台租户初始化和查询测试

---

## 四、平台架构示意图

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                               平台视角                                          │
│                                                                                │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │               Platform Tenant（平台租户, type=PLATFORM）                  │  │
│  │                         code: "__platform__"                            │  │
│  │                         id: 00000000-...-000001                         │  │
│  │                                                                         │  │
│  │   ┌────────────────────────────────────────────────────┐               │  │
│  │   │  OrganizationUnit (平台组织架构)                    │               │  │
│  │   │    └─ 运营中心                                     │               │  │
│  │   │       └─ 客户成功组                                │               │  │
│  │   │       └─ 技术支持组                                │               │  │
│  │   │    └─ 产品中心                                     │               │  │
│  │   │       └─ 产品组                                    │               │  │
│  │   │       └─ 设计组                                    │               │  │
│  │   └────────────────────────────────────────────────────┘               │  │
│  │                                                                         │  │
│  │   平台角色：PLATFORM_ADMIN, TENANT_MANAGER, SUPPORT_STAFF              │  │
│  │   平台权限：tenant:create, tenant:disable, user:view_all ...           │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                                │
│  ┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐        │
│  │ Tenant A           │ │ Tenant B           │ │ Tenant C           │        │
│  │ type=CUSTOMER      │ │ type=CUSTOMER      │ │ type=PARTNER       │        │
│  │ code="acme"        │ │ code="globex"      │ │ code="partner_x"   │        │
│  │                    │ │                    │ │                    │        │
│  │ ┌────────────────┐ │ │ ┌────────────────┐ │ │ ┌────────────────┐ │        │
│  │ │ 组织架构       │ │ │ │ 组织架构       │ │ │ │ 组织架构       │ │        │
│  │ │ └─ 研发部     │ │ │ │ └─ 销售部     │ │ │ │ └─ 技术团队   │ │        │
│  │ │    └─ 前端   │ │ │ │    └─ 华东区 │ │ │ └────────────────┘ │        │
│  │ │    └─ 后端   │ │ │ │    └─ 华南区 │ │ │                    │        │
│  │ │ └─ 市场部     │ │ │ └────────────────┘ │ │ 角色/权限独立     │        │
│  │ └────────────────┘ │ │                    │ └────────────────────┘        │
│  │                    │ │ 角色/权限独立     │                                │
│  │ 角色/权限独立     │ └────────────────────┘                                │
│  └────────────────────┘                                                       │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、API 端点汇总

### 5.1 租户管理（已有 + 新增）

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/tenants` | 创建租户 | ✅ 已有 |
| GET | `/api/tenants` | 查询所有租户 | ✅ 已有 |
| GET | `/api/tenants/business` | 查询业务租户（排除平台） | ⭐ 新增 |
| GET | `/api/tenants/platform` | 获取平台租户信息 | ⭐ 新增 |
| POST | `/api/tenants/switch` | 切换租户 | ✅ 已有 |

### 5.2 组织管理（已有 + 新增）

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/tenants/org-units` | 查询当前租户组织列表 | ✅ 已有 |
| GET | `/api/tenants/org-tree` | 获取组织树结构 | ⭐ 新增 |
| POST | `/api/tenants/org-units` | 创建组织单元 | ⭐ 新增 |
| PUT | `/api/tenants/org-units/{id}` | 更新组织单元 | ⭐ 新增 |
| DELETE | `/api/tenants/org-units/{id}` | 删除组织单元 | ⭐ 新增 |

---

## 六、TDD 执行检查清单

### 阶段六执行清单（遵循 Red→Green→Refactor）

#### 任务 6.1：Tenant 类型支持 [P0]

| 步骤 | 操作 | 完成 |
|------|------|------|
| 🔴 Red | 编写 `TenantTypeTest.java` | [ ] |
| 🔴 Red | 编写 `TenantRepositoryIT.java` | [ ] |
| 🔴 Red | 运行测试，确认失败 | [ ] |
| 🟢 Green | 创建 `V5__tenant_type_and_platform.sql` | [ ] |
| 🟢 Green | 修改 `Tenant.java` 添加 TenantType 枚举和字段 | [ ] |
| 🟢 Green | 修改 `TenantRepository.java` 添加查询方法 | [ ] |
| 🟢 Green | 运行测试，确认通过 | [ ] |
| 🔵 Refactor | 检查代码风格，优化命名 | [ ] |
| ✅ Done | 提交代码：`feat: add tenant type and platform tenant` | [ ] |

#### 任务 6.2：平台租户服务 [P0]

| 步骤 | 操作 | 完成 |
|------|------|------|
| 🔴 Red | 编写 `PlatformTenantServiceTest.java` | [ ] |
| 🔴 Red | 编写 `TenantControllerPlatformIT.java` | [ ] |
| 🔴 Red | 运行测试，确认失败 | [ ] |
| 🟢 Green | 创建 `PlatformTenantService.java` | [ ] |
| 🟢 Green | 扩展 `MembershipRepository.java` | [ ] |
| 🟢 Green | 扩展 `TenantService.java` | [ ] |
| 🟢 Green | 扩展 `TenantController.java` | [ ] |
| 🟢 Green | 运行测试，确认通过 | [ ] |
| 🔵 Refactor | 检查代码风格，提取公共逻辑 | [ ] |
| ✅ Done | 提交代码：`feat: add platform tenant service` | [ ] |

#### 任务 6.3：组织管理 API [P1]

| 步骤 | 操作 | 完成 |
|------|------|------|
| 🔴 Red | 编写 `OrganizationServiceTest.java` | [ ] |
| 🔴 Red | 编写 `OrgUnitControllerIT.java` | [ ] |
| 🔴 Red | 运行测试，确认失败 | [ ] |
| 🟢 Green | 创建 `V6__org_sort_order.sql` | [ ] |
| 🟢 Green | 修改 `OrganizationUnit.java` 添加 sortOrder | [ ] |
| 🟢 Green | 扩展 `OrganizationUnitRepository.java` | [ ] |
| 🟢 Green | 创建 `OrganizationService.java` | [ ] |
| 🟢 Green | 创建 DTO（OrgTreeNode, CreateOrgUnitRequest, UpdateOrgUnitRequest） | [ ] |
| 🟢 Green | 扩展 `TenantController.java` 添加组织端点 | [ ] |
| 🟢 Green | 运行测试，确认通过 | [ ] |
| 🔵 Refactor | 优化组织树构建算法 | [ ] |
| ✅ Done | 提交代码：`feat: add organization management API` | [ ] |

#### 任务 6.4：MembershipRepository 扩展 [P1]

| 步骤 | 操作 | 完成 |
|------|------|------|
| 🔴 Red | 编写 `MembershipRepositoryIT.java` | [ ] |
| 🟢 Green | 添加 `existsByOrgUnitId` 方法 | [ ] |
| 🟢 Green | 添加 `findByOrgUnitId` 方法 | [ ] |
| ✅ Done | 提交代码：`feat: extend membership repository` | [ ] |

#### 任务 6.5：最终验收 [P1]

| 步骤 | 操作 | 完成 |
|------|------|------|
| 🧪 | 运行全量单元测试：`mvn test` | [ ] |
| 🧪 | 运行集成测试：`mvn verify -pl user-center/uc-start` | [ ] |
| 📖 | 验证 Swagger 文档完整性 | [ ] |
| 🚀 | 本地启动应用验证 | [ ] |
| ✅ | 合并到主分支 | [ ] |

---

---

## 七、TDD 快速执行指南

### 7.1 开发环境准备

```bash
# 确保 Docker 运行（用于 Testcontainers）
docker info

# 设置环境变量（macOS Docker Desktop）
export DOCKER_HOST=unix:///Users/$USER/.docker/run/docker.sock

# 验证构建通过
mvn clean compile -DskipTests
```

### 7.2 单任务 TDD 循环示例（以 6.1 为例）

```bash
# ==================== 🔴 RED 阶段 ====================

# 1. 创建测试文件
mkdir -p user-center/uc-domain-tenant/src/test/java/com/company/usercenter/tenant

# 2. 编写测试代码（见 Step 2）

# 3. 运行测试，确认失败
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest
# 预期：编译失败或测试失败 ❌

# ==================== 🟢 GREEN 阶段 ====================

# 4. 创建迁移脚本
touch user-center/uc-start/src/main/resources/db/migration/V5__tenant_type_and_platform.sql
# 编写 SQL 内容

# 5. 修改实体类
# 编辑 Tenant.java，添加 TenantType 枚举和字段

# 6. 修改 Repository
# 编辑 TenantRepository.java，添加查询方法

# 7. 再次运行测试，确认通过
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest
# 预期：测试通过 ✅

# ==================== 🔵 REFACTOR 阶段 ====================

# 8. 检查代码质量
mvn checkstyle:check -pl user-center/uc-domain-tenant

# 9. 运行完整模块测试确保没有破坏
mvn test -pl user-center/uc-domain-tenant

# ==================== ✅ DONE ====================

# 10. 提交代码
git add .
git commit -m "feat: add tenant type and platform tenant support"
```

### 7.3 常用测试命令

```bash
# 运行单个测试类
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest

# 运行单个测试方法
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest#newTenant_shouldHaveDefaultType_CUSTOMER

# 运行某模块所有测试
mvn test -pl user-center/uc-domain-tenant

# 运行集成测试（需要 Docker）
mvn verify -pl user-center/uc-start -Dit.test=TenantControllerPlatformIT

# 运行全量测试
mvn verify

# 生成测试覆盖率报告
mvn jacoco:report -pl user-center/uc-domain-tenant
# 报告位置：target/site/jacoco/index.html
```

### 7.4 TDD 开发心法

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          TDD 黄金法则                                        │
│                                                                             │
│  1. 🚫 不写任何生产代码，除非已有一个失败的测试                               │
│                                                                             │
│  2. 🚫 不写超过让测试失败所需的测试代码                                       │
│                                                                             │
│  3. 🚫 不写超过让测试通过所需的生产代码                                       │
│                                                                             │
│  4. ✅ 先写最简单的测试，从边界条件开始                                       │
│                                                                             │
│  5. ✅ 每次只关注一个功能点，小步前进                                         │
│                                                                             │
│  6. ✅ 测试通过后立即重构，保持代码整洁                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.5 测试用例设计原则

| 原则 | 说明 | 示例 |
|------|------|------|
| **正向路径** | 测试正常流程 | 创建组织成功 |
| **边界条件** | 测试边界值 | 空名称、超长名称 |
| **异常路径** | 测试异常情况 | 父组织不存在、重复创建 |
| **状态验证** | 验证状态变化 | 删除后不可查询 |
| **权限校验** | 测试权限控制 | 跨租户访问应拒绝 |

---

## 八、后续迭代（P2）

以下功能在当前阶段暂不实现，记录以便后续规划：

1. **OrganizationUnit 物化路径**
   - 添加 `path` 和 `level` 字段
   - 实现快速祖先/后代查询

2. **Membership.roles 改为关联表**
   - 创建 `membership_roles` 关联表
   - 实现更精细的角色管理

3. **组织移动功能**
   - 支持将组织移动到其他父节点
   - 级联更新子组织的 path

4. **组织权限继承**
   - 实现基于组织层级的权限继承机制

---

## 附录：相关文件清单

| 文件 | 说明 |
|------|------|
| `uc-start/.../db/migration/V5__tenant_type_and_platform.sql` | 数据库迁移 |
| `uc-domain-tenant/.../Tenant.java` | 租户实体（修改） |
| `uc-domain-tenant/.../TenantRepository.java` | 租户仓储（修改） |
| `uc-domain-tenant/.../TenantService.java` | 租户服务（修改） |
| `uc-domain-tenant/.../PlatformTenantService.java` | 平台租户服务（新增） |
| `uc-domain-tenant/.../OrganizationUnit.java` | 组织实体（修改） |
| `uc-domain-tenant/.../OrganizationUnitRepository.java` | 组织仓储（修改） |
| `uc-domain-tenant/.../OrganizationService.java` | 组织服务（新增） |
| `uc-domain-identity/.../MembershipRepository.java` | 成员仓储（修改） |
| `uc-start/.../TenantController.java` | 租户控制器（修改） |
| `uc-api/.../dto/OrgTreeNode.java` | 组织树 DTO（新增） |
| `uc-api/.../dto/CreateOrgUnitRequest.java` | 创建组织请求（新增） |
| `uc-api/.../dto/UpdateOrgUnitRequest.java` | 更新组织请求（新增） |
