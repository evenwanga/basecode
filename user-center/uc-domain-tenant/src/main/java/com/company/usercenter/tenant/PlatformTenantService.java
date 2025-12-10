package com.company.usercenter.tenant;

import com.company.usercenter.identity.Membership;
import com.company.usercenter.identity.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 平台租户服务
 * 提供平台租户相关的查询和管理功能
 */
@Service
public class PlatformTenantService {

    /** 平台租户编码（系统唯一） */
    public static final String PLATFORM_TENANT_CODE = "__platform__";

    /** 平台租户 ID（固定 UUID） */
    public static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TenantRepository tenantRepository;
    private final MembershipRepository membershipRepository;

    public PlatformTenantService(TenantRepository tenantRepository, MembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    /**
     * 获取平台租户
     *
     * @return 平台租户实例
     * @throws IllegalStateException 如果平台租户未初始化
     */
    public Tenant getPlatformTenant() {
        return tenantRepository.findByCode(PLATFORM_TENANT_CODE)
            .orElseThrow(() -> new IllegalStateException("平台租户未初始化"));
    }

    /**
     * 判断指定租户是否为平台租户
     *
     * @param tenantId 租户 ID
     * @return 是否为平台租户
     */
    public boolean isPlatformTenant(UUID tenantId) {
        return PLATFORM_TENANT_ID.equals(tenantId);
    }

    /**
     * 判断用户是否为平台成员
     *
     * @param userId 用户 ID
     * @return 是否为平台成员
     */
    public boolean isPlatformMember(UUID userId) {
        return membershipRepository.existsByUserIdAndTenantId(userId, PLATFORM_TENANT_ID);
    }

    /**
     * 判断用户是否为平台管理员
     *
     * @param userId 用户 ID
     * @return 是否为平台管理员（拥有 PLATFORM_ADMIN 角色）
     */
    public boolean isPlatformAdmin(UUID userId) {
        return membershipRepository.findByUserIdAndTenantId(userId, PLATFORM_TENANT_ID)
            .map(m -> m.getRoles() != null && m.getRoles().contains("PLATFORM_ADMIN"))
            .orElse(false);
    }

    /**
     * 将用户添加为平台成员
     *
     * @param userId 用户 ID
     * @param roles 角色列表（逗号分隔）
     * @return 创建的成员关系
     * @throws IllegalArgumentException 如果用户已是平台成员
     */
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
