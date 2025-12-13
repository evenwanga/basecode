package com.company.usercenter.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * MembershipRepository 方法存在性测试
 * TDD 6.2: 验证 Repository 接口方法定义正确
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipRepository 方法测试")
class MembershipRepositoryTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Test
    @DisplayName("existsByUserIdAndTenantId 方法应存在且返回正确类型")
    void existsByUserIdAndTenantId_shouldExistAndReturnBoolean() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        when(membershipRepository.existsByUserIdAndTenantId(userId, tenantId)).thenReturn(true);
        
        boolean result = membershipRepository.existsByUserIdAndTenantId(userId, tenantId);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("findByUserIdAndTenantId 方法应存在且返回 Optional<Membership>")
    void findByUserIdAndTenantId_shouldExistAndReturnOptional() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setTenantId(tenantId);
        membership.setRoles("USER,ADMIN");
        
        when(membershipRepository.findByUserIdAndTenantId(userId, tenantId))
            .thenReturn(Optional.of(membership));
        
        Optional<Membership> result = membershipRepository.findByUserIdAndTenantId(userId, tenantId);
        
        assertThat(result).isPresent();
        assertThat(result.get().getRoles()).isEqualTo("USER,ADMIN");
    }

    @Test
    @DisplayName("existsByOrgUnitId 方法应存在且返回正确类型")
    void existsByOrgUnitId_shouldExistAndReturnBoolean() {
        UUID orgUnitId = UUID.randomUUID();
        
        when(membershipRepository.existsByOrgUnitId(orgUnitId)).thenReturn(true);
        
        boolean result = membershipRepository.existsByOrgUnitId(orgUnitId);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("findByOrgUnitId 方法应存在且返回列表")
    void findByOrgUnitId_shouldExistAndReturnList() {
        UUID orgUnitId = UUID.randomUUID();
        Membership membership = new Membership();
        membership.setOrgUnitId(orgUnitId);
        
        when(membershipRepository.findByOrgUnitId(orgUnitId))
            .thenReturn(List.of(membership));
        
        List<Membership> result = membershipRepository.findByOrgUnitId(orgUnitId);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrgUnitId()).isEqualTo(orgUnitId);
    }

    @Test
    @DisplayName("existsByUserIdAndTenantId 不存在时应返回 false")
    void existsByUserIdAndTenantId_shouldReturnFalse_whenNotExists() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        when(membershipRepository.existsByUserIdAndTenantId(userId, tenantId)).thenReturn(false);
        
        boolean result = membershipRepository.existsByUserIdAndTenantId(userId, tenantId);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("findByUserIdAndTenantId 不存在时应返回空 Optional")
    void findByUserIdAndTenantId_shouldReturnEmpty_whenNotExists() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        when(membershipRepository.findByUserIdAndTenantId(userId, tenantId))
            .thenReturn(Optional.empty());
        
        Optional<Membership> result = membershipRepository.findByUserIdAndTenantId(userId, tenantId);
        assertThat(result).isEmpty();
    }
}




