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

        @Test
        @DisplayName("用户不是平台成员时应返回false")
        void shouldReturnFalse_whenNotMember() {
            UUID userId = UUID.randomUUID();
            when(membershipRepository.findByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(Optional.empty());

            assertThat(service.isPlatformAdmin(userId)).isFalse();
        }
    }

    @Nested
    @DisplayName("addPlatformMember")
    class AddPlatformMember {

        @Test
        @DisplayName("新用户应成功添加为平台成员")
        void shouldAddNewMember_successfully() {
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(false);
            when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

            Membership result = service.addPlatformMember(userId, "USER");

            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getTenantId()).isEqualTo(PlatformTenantService.PLATFORM_TENANT_ID);
            assertThat(result.getRoles()).isEqualTo("USER");
        }

        @Test
        @DisplayName("已存在的平台成员应抛出异常")
        void shouldThrowException_whenAlreadyPlatformMember() {
            UUID userId = UUID.randomUUID();
            when(membershipRepository.existsByUserIdAndTenantId(userId, PlatformTenantService.PLATFORM_TENANT_ID))
                .thenReturn(true);

            assertThatThrownBy(() -> service.addPlatformMember(userId, "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户已是平台成员");
        }
    }
}
