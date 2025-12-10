package com.company.usercenter.tenant;

import com.company.usercenter.api.dto.OrgTreeNode;
import com.company.usercenter.identity.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织管理服务
 */
@Service
public class OrganizationService {

    private final OrganizationUnitRepository orgRepository;
    private final MembershipRepository membershipRepository;

    public OrganizationService(OrganizationUnitRepository orgRepository, MembershipRepository membershipRepository) {
        this.orgRepository = orgRepository;
        this.membershipRepository = membershipRepository;
    }

    /**
     * 创建组织单元
     *
     * @param tenantId 租户ID
     * @param parentId 父组织ID，为空则创建顶级组织
     * @param name     组织名称
     * @return 创建的组织单元
     */
    @Transactional
    public OrganizationUnit createOrgUnit(UUID tenantId, UUID parentId, String name) {
        // 验证父组织
        if (parentId != null) {
            OrganizationUnit parent = orgRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("父组织不存在"));
            if (!parent.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("父组织不属于当前租户");
            }
        }

        // 计算排序序号
        int sortOrder = orgRepository.countByParentId(parentId);

        // 创建组织单元
        OrganizationUnit org = new OrganizationUnit();
        org.setTenantId(tenantId);
        org.setParentId(parentId);
        org.setName(name);
        org.setStatus("ACTIVE");
        org.setSortOrder(sortOrder);

        return orgRepository.save(org);
    }

    /**
     * 更新组织单元
     *
     * @param orgId     组织ID
     * @param name      新名称，为空则不更新
     * @param sortOrder 新排序序号，为空则不更新
     * @return 更新后的组织单元
     */
    @Transactional
    public OrganizationUnit updateOrgUnit(UUID orgId, String name, Integer sortOrder) {
        OrganizationUnit org = orgRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

        if (name != null) {
            org.setName(name);
        }
        if (sortOrder != null) {
            org.setSortOrder(sortOrder);
        }

        return orgRepository.save(org);
    }

    /**
     * 删除组织单元
     *
     * @param orgId 组织ID
     */
    @Transactional
    public void deleteOrgUnit(UUID orgId) {
        OrganizationUnit org = orgRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

        // 检查是否有子组织
        if (orgRepository.existsByTenantIdAndParentId(org.getTenantId(), orgId)) {
            throw new IllegalStateException("存在子组织，无法删除");
        }

        // 检查是否有成员
        if (membershipRepository.existsByOrgUnitId(orgId)) {
            throw new IllegalStateException("组织下存在成员，无法删除");
        }

        orgRepository.delete(org);
    }

    /**
     * 获取组织树
     *
     * @param tenantId 租户ID
     * @return 组织树结构
     */
    public List<OrgTreeNode> getOrgTree(UUID tenantId) {
        List<OrganizationUnit> allOrgs = orgRepository.findByTenantId(tenantId);
        return buildTree(allOrgs, null);
    }

    /**
     * 递归构建组织树
     */
    private List<OrgTreeNode> buildTree(List<OrganizationUnit> allOrgs, UUID parentId) {
        return allOrgs.stream()
                .filter(o -> Objects.equals(o.getParentId(), parentId))
                .sorted(Comparator.comparingInt(OrganizationUnit::getSortOrder))
                .map(o -> new OrgTreeNode(
                        o.getId(),
                        o.getName(),
                        o.getStatus(),
                        buildTree(allOrgs, o.getId())
                ))
                .collect(Collectors.toList());
    }
}
