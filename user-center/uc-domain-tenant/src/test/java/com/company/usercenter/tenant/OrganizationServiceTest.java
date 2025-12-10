package com.company.usercenter.tenant;

import com.company.usercenter.api.dto.OrgTreeNode;
import com.company.usercenter.identity.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrganizationService 单元测试
 * 测试组织管理的核心业务逻辑
 */
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
                setEntityId(org, UUID.randomUUID());
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
            //   ├── 市场部 (sortOrder=0)
            //   └── 研发部 (sortOrder=1)
            //       ├── 前端组 (sortOrder=0)
            //       └── 后端组 (sortOrder=1)
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
        setEntityId(org, id);
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

    /**
     * 使用反射设置实体ID（仅用于测试）
     */
    private static void setEntityId(Object entity, UUID id) {
        try {
            Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity id", e);
        }
    }
}
