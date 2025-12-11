# TDD 任务执行卡片

> 🎯 每张卡片代表一个独立的 TDD 开发任务，按顺序执行即可完成第六阶段开发。
>
> ⚠️ **任务已按依赖关系排序**，请严格按顺序执行。

---

## 📋 任务总览与依赖关系

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  第六阶段：平台租户与组织架构增强                                                  │
│                                                                                 │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐       │
│  │  6.1    │───►│  6.2    │───►│  6.3    │───►│  6.4    │───►│  6.5    │       │
│  │ Tenant  │    │ Member  │    │Platform │    │  Org    │    │ Final   │       │
│  │  Type   │    │  Repo   │    │Service  │    │  CRUD   │    │ Verify  │       │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘       │
│     2h            1h             3h            12h            2h               │
│                                                                                 │
│  依赖关系：                                                                      │
│  • 6.1 → 无依赖（入口）                                                          │
│  • 6.2 → 依赖 6.1                                                               │
│  • 6.3 → 依赖 6.1, 6.2                                                          │
│  • 6.4 → 依赖 6.2, 6.3                                                          │
│  • 6.5 → 依赖 6.1-6.4                                                           │
│                                                                                 │
│  💡 6.3 和 6.4 理论上可并行（如有多人协作）                                        │
│                                                                                 │
│  总预估：20h (约 2.5 个工作日)                                                    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🃏 卡片 6.1：Tenant 类型支持

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **预估工时** | 2h |
| **依赖** | 无 |
| **被依赖** | 6.2, 6.3, 6.4, 6.5 |

### 验收标准

- [ ] Tenant 实体包含 `type` 字段，默认值为 `CUSTOMER`
- [ ] 支持 `PLATFORM/INTERNAL/CUSTOMER/PARTNER` 四种类型
- [ ] 平台租户通过迁移脚本自动初始化
- [ ] 可按 type 查询租户列表

### TDD 执行步骤

#### 🔴 RED（编写失败的测试）

**创建测试文件**：`uc-domain-tenant/src/test/java/com/company/usercenter/tenant/TenantTypeTest.java`

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
        assertThat(Tenant.TenantType.values()).containsExactlyInAnyOrder(
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

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest
# 预期：编译失败 ❌
```

#### 🟢 GREEN（实现代码）

**1. 修改 Tenant.java** - 添加枚举和字段：

```java
// 在 Tenant 类中添加

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

**2. 创建迁移脚本** `V5__tenant_type_and_platform.sql`：

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

**3. 扩展 TenantRepository.java**：

```java
List<Tenant> findByType(Tenant.TenantType type);
List<Tenant> findByTypeNot(Tenant.TenantType type);
Optional<Tenant> findFirstByType(Tenant.TenantType type);
```

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest
# 预期：测试通过 ✅
```

#### 🔵 REFACTOR

- 确保 Swagger 注解完整
- 检查代码风格

#### ✅ 提交

```bash
git add .
git commit -m "feat(tenant): add tenant type support and platform tenant initialization"
```

---

## 🃏 卡片 6.2：MembershipRepository 扩展

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **预估工时** | 1h |
| **依赖** | 6.1 |
| **被依赖** | 6.3, 6.4 |

### 验收标准

- [ ] 支持 `existsByUserIdAndTenantId` 查询
- [ ] 支持 `findByUserIdAndTenantId` 查询
- [ ] 支持 `existsByOrgUnitId` 查询
- [ ] 支持 `findByOrgUnitId` 查询

### TDD 执行步骤

#### 🔴 RED

**创建测试文件**：`uc-domain-identity/src/test/java/com/company/usercenter/identity/MembershipRepositoryTest.java`

```java
package com.company.usercenter.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@DisplayName("MembershipRepository 测试")
class MembershipRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    @DisplayName("existsByUserIdAndTenantId 应正确判断成员关系存在")
    void existsByUserIdAndTenantId_shouldReturnTrue_whenExists() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setTenantId(tenantId);
        membership.setStatus("ACTIVE");
        membershipRepository.save(membership);

        assertThat(membershipRepository.existsByUserIdAndTenantId(userId, tenantId)).isTrue();
        assertThat(membershipRepository.existsByUserIdAndTenantId(userId, UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("findByUserIdAndTenantId 应返回成员关系")
    void findByUserIdAndTenantId_shouldReturnMembership() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setTenantId(tenantId);
        membership.setRoles("USER,ADMIN");
        membership.setStatus("ACTIVE");
        membershipRepository.save(membership);

        Optional<Membership> found = membershipRepository.findByUserIdAndTenantId(userId, tenantId);
        
        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).isEqualTo("USER,ADMIN");
    }

    @Test
    @DisplayName("existsByOrgUnitId 应正确判断组织下是否有成员")
    void existsByOrgUnitId_shouldReturnCorrectResult() {
        UUID orgUnitId = UUID.randomUUID();
        
        Membership membership = new Membership();
        membership.setUserId(UUID.randomUUID());
        membership.setTenantId(UUID.randomUUID());
        membership.setOrgUnitId(orgUnitId);
        membership.setStatus("ACTIVE");
        membershipRepository.save(membership);

        assertThat(membershipRepository.existsByOrgUnitId(orgUnitId)).isTrue();
        assertThat(membershipRepository.existsByOrgUnitId(UUID.randomUUID())).isFalse();
    }
}
```

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-identity -Dtest=MembershipRepositoryTest
# 预期：编译失败（方法不存在）❌
```

#### 🟢 GREEN

**修改 MembershipRepository.java**：

```java
package com.company.usercenter.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(UUID userId);
    
    // 新增方法
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
    Optional<Membership> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    boolean existsByOrgUnitId(UUID orgUnitId);
    List<Membership> findByOrgUnitId(UUID orgUnitId);
}
```

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-identity -Dtest=MembershipRepositoryTest
# 预期：测试通过 ✅
```

#### ✅ 提交

```bash
git add .
git commit -m "feat(identity): extend MembershipRepository with tenant and org queries"
```

---

## 🃏 卡片 6.3：平台租户服务

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **预估工时** | 3h |
| **依赖** | 6.1, 6.2 |
| **被依赖** | 6.5 |
| **可并行** | 与 6.4 并行（如有多人） |

### 验收标准

- [ ] `PlatformTenantService` 提供获取平台租户方法
- [ ] 能判断某租户是否为平台租户
- [ ] 能判断用户是否为平台成员/管理员
- [ ] API `/api/tenants/platform` 返回平台租户
- [ ] API `/api/tenants/business` 返回非平台租户

### TDD 执行步骤

#### 🔴 RED

**创建测试文件**：`uc-domain-tenant/src/test/java/com/company/usercenter/tenant/PlatformTenantServiceTest.java`

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformTenantService 单元测试")
class PlatformTenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private MembershipRepository membershipRepository;

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
        void shouldReturn_whenExists() {
            Tenant platform = new Tenant();
            platform.setCode("__platform__");
            when(tenantRepository.findByCode("__platform__")).thenReturn(Optional.of(platform));

            assertThat(service.getPlatformTenant().getCode()).isEqualTo("__platform__");
        }

        @Test
        @DisplayName("平台租户不存在时应抛出异常")
        void shouldThrow_whenNotExists() {
            when(tenantRepository.findByCode("__platform__")).thenReturn(Optional.empty());

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
        void shouldReturnTrue_forPlatformId() {
            assertThat(service.isPlatformTenant(PlatformTenantService.PLATFORM_TENANT_ID)).isTrue();
        }

        @Test
        @DisplayName("非平台租户ID应返回false")
        void shouldReturnFalse_forOtherId() {
            assertThat(service.isPlatformTenant(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    @DisplayName("isPlatformMember")
    class IsPlatformMember {

        @Test
        @DisplayName("用户是平台成员时应返回true")
        void shouldReturnTrue_whenIsMember() {
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(true);

            assertThat(service.isPlatformMember(userId)).isTrue();
        }

        @Test
        @DisplayName("用户不是平台成员时应返回false")
        void shouldReturnFalse_whenNotMember() {
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(false);

            assertThat(service.isPlatformMember(userId)).isFalse();
        }
    }

    @Nested
    @DisplayName("isPlatformAdmin")
    class IsPlatformAdmin {

        @Test
        @DisplayName("用户有PLATFORM_ADMIN角色时应返回true")
        void shouldReturnTrue_whenHasAdminRole() {
            UUID userId = UUID.randomUUID();
            Membership membership = new Membership();
            membership.setRoles("PLATFORM_ADMIN,USER");
            when(membershipRepository.findByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(Optional.of(membership));

            assertThat(service.isPlatformAdmin(userId)).isTrue();
        }

        @Test
        @DisplayName("用户无PLATFORM_ADMIN角色时应返回false")
        void shouldReturnFalse_whenLacksAdminRole() {
            UUID userId = UUID.randomUUID();
            Membership membership = new Membership();
            membership.setRoles("USER");
            when(membershipRepository.findByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(Optional.of(membership));

            assertThat(service.isPlatformAdmin(userId)).isFalse();
        }
    }
}
```

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-tenant -Dtest=PlatformTenantServiceTest
# 预期：编译失败 ❌
```

#### 🟢 GREEN

**1. 创建 PlatformTenantService.java**：

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

**2. 扩展 TenantService.java**：

```java
// 添加方法
public List<Tenant> listBusinessTenants() {
    return tenantRepository.findByTypeNot(Tenant.TenantType.PLATFORM);
}
```

**3. 扩展 TenantController.java**：

```java
private final PlatformTenantService platformTenantService;

// 构造函数注入 platformTenantService

@GetMapping("/platform")
@Operation(summary = "获取平台租户", description = "返回平台租户信息")
public ApiResponse<Tenant> getPlatformTenant() {
    return ApiResponse.ok(platformTenantService.getPlatformTenant());
}

@GetMapping("/business")
@Operation(summary = "查询业务租户列表", description = "返回除平台租户外的所有租户")
public ApiResponse<List<Tenant>> listBusinessTenants() {
    return ApiResponse.ok(tenantService.listBusinessTenants());
}
```

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-tenant -Dtest=PlatformTenantServiceTest
# 预期：测试通过 ✅
```

#### ✅ 提交

```bash
git add .
git commit -m "feat(tenant): add platform tenant service and API endpoints"
```

---

## 🃏 卡片 6.4：组织管理 API

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **预估工时** | 12h |
| **依赖** | 6.2 |
| **被依赖** | 6.5 |
| **可并行** | 与 6.3 并行（如有多人） |

### 验收标准

- [ ] 能创建顶级组织（parentId=null）
- [ ] 能创建子组织
- [ ] 父组织不存在时抛异常
- [ ] 父组织属于其他租户时抛异常
- [ ] 能更新组织名称和排序
- [ ] 能删除无子组织且无成员的组织
- [ ] 有子组织时禁止删除
- [ ] 有成员时禁止删除
- [ ] 能获取树形结构组织

### TDD 执行步骤

#### 🔴 RED

**创建测试文件**：`uc-domain-tenant/src/test/java/com/company/usercenter/tenant/OrganizationServiceTest.java`

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

    @Mock private OrganizationUnitRepository orgRepository;
    @Mock private MembershipRepository membershipRepository;

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
            when(orgRepository.countByParentId(null)).thenReturn(0);
            when(orgRepository.save(any())).thenAnswer(inv -> {
                OrganizationUnit org = inv.getArgument(0);
                org.setId(UUID.randomUUID());
                return org;
            });

            OrganizationUnit result = service.createOrgUnit(tenantId, null, "研发部");

            assertThat(result.getName()).isEqualTo("研发部");
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            assertThat(result.getParentId()).isNull();
        }

        @Test
        @DisplayName("应能创建子组织")
        void shouldCreateChildOrg() {
            UUID parentId = UUID.randomUUID();
            OrganizationUnit parent = createOrg(parentId, tenantId, null, "根组织");
            when(orgRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(orgRepository.countByParentId(parentId)).thenReturn(2);
            when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationUnit result = service.createOrgUnit(tenantId, parentId, "前端组");

            assertThat(result.getParentId()).isEqualTo(parentId);
            assertThat(result.getSortOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("父组织不存在时应抛异常")
        void shouldThrowWhenParentNotExists() {
            UUID parentId = UUID.randomUUID();
            when(orgRepository.findById(parentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrgUnit(tenantId, parentId, "子组织"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("父组织不存在");
        }

        @Test
        @DisplayName("父组织属于其他租户时应抛异常")
        void shouldThrowWhenParentBelongsToOtherTenant() {
            UUID parentId = UUID.randomUUID();
            UUID otherTenantId = UUID.randomUUID();
            OrganizationUnit parent = createOrg(parentId, otherTenantId, null, "其他租户组织");
            when(orgRepository.findById(parentId)).thenReturn(Optional.of(parent));

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
        void shouldUpdateName() {
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "旧名称");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationUnit result = service.updateOrgUnit(orgId, "新名称", null);

            assertThat(result.getName()).isEqualTo("新名称");
        }

        @Test
        @DisplayName("组织不存在时应抛异常")
        void shouldThrowWhenNotExists() {
            UUID orgId = UUID.randomUUID();
            when(orgRepository.findById(orgId)).thenReturn(Optional.empty());

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
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "待删除");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.existsByTenantIdAndParentId(tenantId, orgId)).thenReturn(false);
            when(membershipRepository.existsByOrgUnitId(orgId)).thenReturn(false);

            service.deleteOrgUnit(orgId);

            verify(orgRepository).delete(org);
        }

        @Test
        @DisplayName("有子组织时应拒绝删除")
        void shouldRejectWhenHasChildren() {
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "有子组织");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.existsByTenantIdAndParentId(tenantId, orgId)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteOrgUnit(orgId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("存在子组织");
        }

        @Test
        @DisplayName("有成员时应拒绝删除")
        void shouldRejectWhenHasMembers() {
            UUID orgId = UUID.randomUUID();
            OrganizationUnit org = createOrg(orgId, tenantId, null, "有成员");
            when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(orgRepository.existsByTenantIdAndParentId(tenantId, orgId)).thenReturn(false);
            when(membershipRepository.existsByOrgUnitId(orgId)).thenReturn(true);

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
        void shouldReturnCorrectTree() {
            UUID rootId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            List<OrganizationUnit> orgs = List.of(
                createOrgWithSort(rootId, tenantId, null, "Root", 0),
                createOrgWithSort(childId, tenantId, rootId, "子部门", 0)
            );
            when(orgRepository.findByTenantId(tenantId)).thenReturn(orgs);

            List<OrgTreeNode> tree = service.getOrgTree(tenantId);

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).name()).isEqualTo("Root");
            assertThat(tree.get(0).children()).hasSize(1);
            assertThat(tree.get(0).children().get(0).name()).isEqualTo("子部门");
        }

        @Test
        @DisplayName("空租户应返回空列表")
        void shouldReturnEmptyForEmptyTenant() {
            when(orgRepository.findByTenantId(tenantId)).thenReturn(Collections.emptyList());

            List<OrgTreeNode> tree = service.getOrgTree(tenantId);

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

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-tenant -Dtest=OrganizationServiceTest
# 预期：编译失败 ❌
```

#### 🟢 GREEN

**1. 创建迁移脚本** `V6__org_sort_order.sql`：

```sql
ALTER TABLE organization_units ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
```

**2. 修改 OrganizationUnit.java**：

```java
@Column
private Integer sortOrder = 0;

public Integer getSortOrder() { return sortOrder; }
public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
```

**3. 扩展 OrganizationUnitRepository.java**：

```java
boolean existsByTenantIdAndParentId(UUID tenantId, UUID parentId);
int countByParentId(UUID parentId);
```

**4. 创建 OrgTreeNode.java**（在 uc-api 模块）：

```java
package com.company.usercenter.api.dto;

import java.util.List;
import java.util.UUID;

public record OrgTreeNode(UUID id, String name, String status, List<OrgTreeNode> children) {}
```

**5. 创建 OrganizationService.java**：

```java
package com.company.usercenter.tenant;

import com.company.usercenter.api.dto.OrgTreeNode;
import com.company.usercenter.identity.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private final OrganizationUnitRepository orgRepository;
    private final MembershipRepository membershipRepository;

    public OrganizationService(OrganizationUnitRepository orgRepository, MembershipRepository membershipRepository) {
        this.orgRepository = orgRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public OrganizationUnit createOrgUnit(UUID tenantId, UUID parentId, String name) {
        if (parentId != null) {
            orgRepository.findById(parentId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("父组织不存在或不属于当前租户"));
        }

        OrganizationUnit org = new OrganizationUnit();
        org.setTenantId(tenantId);
        org.setParentId(parentId);
        org.setName(name);
        org.setStatus("ACTIVE");
        org.setSortOrder(orgRepository.countByParentId(parentId));
        return orgRepository.save(org);
    }

    @Transactional
    public OrganizationUnit updateOrgUnit(UUID orgId, String name, Integer sortOrder) {
        OrganizationUnit org = orgRepository.findById(orgId)
            .orElseThrow(() -> new IllegalArgumentException("组织不存在"));
        if (name != null) org.setName(name);
        if (sortOrder != null) org.setSortOrder(sortOrder);
        return orgRepository.save(org);
    }

    @Transactional
    public void deleteOrgUnit(UUID orgId) {
        OrganizationUnit org = orgRepository.findById(orgId)
            .orElseThrow(() -> new IllegalArgumentException("组织不存在"));
        if (orgRepository.existsByTenantIdAndParentId(org.getTenantId(), orgId)) {
            throw new IllegalStateException("存在子组织，无法删除");
        }
        if (membershipRepository.existsByOrgUnitId(orgId)) {
            throw new IllegalStateException("组织下存在成员，无法删除");
        }
        orgRepository.delete(org);
    }

    public List<OrgTreeNode> getOrgTree(UUID tenantId) {
        List<OrganizationUnit> allOrgs = orgRepository.findByTenantId(tenantId);
        return buildTree(allOrgs, null);
    }

    private List<OrgTreeNode> buildTree(List<OrganizationUnit> allOrgs, UUID parentId) {
        return allOrgs.stream()
            .filter(o -> Objects.equals(o.getParentId(), parentId))
            .sorted(Comparator.comparingInt(OrganizationUnit::getSortOrder))
            .map(o -> new OrgTreeNode(o.getId(), o.getName(), o.getStatus(), buildTree(allOrgs, o.getId())))
            .collect(Collectors.toList());
    }
}
```

**6. 创建请求 DTO**（在 uc-api 模块）：

```java
// CreateOrgUnitRequest.java
public record CreateOrgUnitRequest(UUID parentId, @NotBlank String name) {}

// UpdateOrgUnitRequest.java
public record UpdateOrgUnitRequest(String name, Integer sortOrder) {}
```

**7. 扩展 TenantController.java**：

```java
private final OrganizationService organizationService;

// 构造函数注入

@PostMapping("/org-units")
@Operation(summary = "创建组织单元")
public ApiResponse<OrganizationUnit> createOrgUnit(@Valid @RequestBody CreateOrgUnitRequest request) {
    UUID tenantId = UUID.fromString(TenantContext.requireTenantId());
    return ApiResponse.created(organizationService.createOrgUnit(tenantId, request.parentId(), request.name()));
}

@PutMapping("/org-units/{orgId}")
@Operation(summary = "更新组织单元")
public ApiResponse<OrganizationUnit> updateOrgUnit(
        @PathVariable UUID orgId,
        @Valid @RequestBody UpdateOrgUnitRequest request) {
    return ApiResponse.ok(organizationService.updateOrgUnit(orgId, request.name(), request.sortOrder()));
}

@DeleteMapping("/org-units/{orgId}")
@Operation(summary = "删除组织单元")
public ApiResponse<Void> deleteOrgUnit(@PathVariable UUID orgId) {
    organizationService.deleteOrgUnit(orgId);
    return ApiResponse.ok(null);
}

@GetMapping("/org-tree")
@Operation(summary = "获取组织树")
public ApiResponse<List<OrgTreeNode>> getOrgTree() {
    UUID tenantId = UUID.fromString(TenantContext.requireTenantId());
    return ApiResponse.ok(organizationService.getOrgTree(tenantId));
}
```

**运行测试**：
```bash
mvn test -pl user-center/uc-domain-tenant -Dtest=OrganizationServiceTest
# 预期：测试通过 ✅
```

#### ✅ 提交

```bash
git add .
git commit -m "feat(org): add organization management service and API"
```

---

## 🃏 卡片 6.5：最终验收

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **预估工时** | 2h |
| **依赖** | 6.1, 6.2, 6.3, 6.4 |

### 验收清单

```bash
# 1. 运行全量测试
mvn clean verify

# 2. 本地启动应用
mvn spring-boot:run -pl user-center/uc-start -Dspring-boot.run.profiles=local

# 3. 验证 Swagger 文档
open http://localhost:8080/swagger-ui.html

# 4. 手动验证 API
# 获取平台租户
curl http://localhost:8080/api/tenants/platform

# 获取业务租户列表
curl http://localhost:8080/api/tenants/business

# 创建租户
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{"code": "test_corp", "name": "测试公司"}'

# 获取组织树（需要 X-Tenant-Id）
curl -H "X-Tenant-Id: <tenant-id>" http://localhost:8080/api/tenants/org-tree

# 创建组织
curl -X POST http://localhost:8080/api/tenants/org-units \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: <tenant-id>" \
  -d '{"name": "研发部"}'
```

### 提交并合并

```bash
git add .
git commit -m "chore: complete phase 6 - platform tenant and organization management"
git push origin feature/phase-6-platform-org
# 创建 PR 并合并
```

---

## 📊 进度追踪表

| 任务 | 依赖 | 状态 | 开始时间 | 完成时间 | 备注 |
|------|------|------|----------|----------|------|
| 6.1 Tenant Type | - | ✅ 已完成 | 2025-12-10 | 2025-12-10 | TDD完成 |
| 6.2 Membership Repo | 6.1 | ✅ 已完成 | 2025-12-10 | 2025-12-10 | TDD完成 |
| 6.3 Platform Service | 6.1, 6.2 | ✅ 已完成 | 2025-12-11 | 2025-12-11 | TDD完成，11个测试用例 |
| 6.4 Org CRUD | 6.2 | ✅ 已完成 | 2025-12-10 | 2025-12-10 | TDD完成，12个测试通过 |
| 6.5 Final Verify | 6.1-6.4 | ⬜ 待开始 | | | |

**状态图例**：⬜ 待开始 | 🔴 测试编写中 | 🟢 实现中 | 🔵 重构中 | ✅ 已完成

---

## 🛠️ 常用命令速查

```bash
# 运行单个测试类
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest

# 运行单个测试方法
mvn test -pl user-center/uc-domain-tenant -Dtest=TenantTypeTest#newTenant_shouldHaveDefaultType_CUSTOMER

# 运行某模块所有测试
mvn test -pl user-center/uc-domain-tenant

# 运行集成测试
mvn verify -pl user-center/uc-start

# 仅编译不测试
mvn compile -DskipTests

# 查看测试覆盖率
mvn jacoco:report -pl user-center/uc-domain-tenant
open user-center/uc-domain-tenant/target/site/jacoco/index.html
```
