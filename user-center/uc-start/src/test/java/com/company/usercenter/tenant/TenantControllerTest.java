package com.company.usercenter.tenant;

import com.company.platform.common.ApiResponse;
import com.company.usercenter.api.dto.SwitchTenantRequest;
import com.company.usercenter.identity.IdentityService;
import com.company.usercenter.identity.User;
import com.company.usercenter.identity.UserIdentity;
import com.company.usercenter.tenant.PlatformTenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantControllerTest {

    TenantService tenantService = mock(TenantService.class);
    IdentityService identityService = mock(IdentityService.class);
    OrganizationService organizationService = mock(OrganizationService.class);
    PlatformTenantService platformTenantService = mock(PlatformTenantService.class);

    TenantController controller = new TenantController(tenantService, identityService, organizationService,
            platformTenantService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void switchTenantShouldReturnUnauthorizedWhenNoLogin() {
        when(tenantService.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(new Tenant()));
        ApiResponse<Tenant> resp = controller.switchTenant(new SwitchTenantRequest(UUID.randomUUID()));
        assertThat(resp.success()).isFalse();
        assertThat(resp.status()).isEqualTo(401);
    }

    @Test
    void switchTenantShouldReturnForbiddenWhenNotMember() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        setId(tenant, tenantId);
        when(tenantService.findById(tenantId)).thenReturn(Optional.of(tenant));

        User user = new User();
        setId(user, UUID.randomUUID());
        when(identityService.findByIdentifier(tenantId, "alice@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.of(user));
        when(identityService.hasMembership(user.getId(), tenantId)).thenReturn(false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice@example.com", "N/A",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        ApiResponse<Tenant> resp = controller.switchTenant(new SwitchTenantRequest(tenantId));
        assertThat(resp.success()).isFalse();
        assertThat(resp.status()).isEqualTo(403);
    }

    @Test
    void switchTenantShouldPassWhenMember() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        setId(tenant, tenantId);
        when(tenantService.findById(tenantId)).thenReturn(Optional.of(tenant));

        User user = new User();
        setId(user, UUID.randomUUID());
        when(identityService.findByIdentifier(tenantId, "alice@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.of(user));
        when(identityService.hasMembership(user.getId(), tenantId)).thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice@example.com", "N/A",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        ApiResponse<Tenant> resp = controller.switchTenant(new SwitchTenantRequest(tenantId));
        assertThat(resp.success()).isTrue();
        assertThat(resp.data()).isEqualTo(tenant);
    }

    private void setId(Object target, UUID id) {
        try {
            Field field = target.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
