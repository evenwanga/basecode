package com.company.usercenter.identity;

import com.company.platform.common.ApiResponse;
import com.company.usercenter.api.dto.UserProfileResponse;
import com.company.usercenter.identity.service.VerificationCodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityControllerTest {

    IdentityService identityService = mock(IdentityService.class);
    VerificationCodeService verificationCodeService = mock(VerificationCodeService.class);

    IdentityController controller = new IdentityController(identityService, verificationCodeService);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void meShouldReturnUnauthorizedWhenNotLogin() {
        ApiResponse<UserProfileResponse> resp = controller.me();
        assertThat(resp.success()).isFalse();
        assertThat(resp.status()).isEqualTo(401);
    }

    @Test
    void meShouldReturnProfileWhenAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bob@example.com", "N/A",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        UUID tenantId = UUID.randomUUID();
        com.company.platform.jpa.TenantContext.setTenantId(tenantId.toString());

        User user = new User();
        setId(user, UUID.randomUUID());
        user.setDisplayName("Bob");
        user.setPrimaryEmail("bob@example.com");
        user.setStatus("ACTIVE");

        // Use any() for tenantId matcher if needed, or specific tenantId
        when(identityService.findByIdentifier(org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.eq("bob@example.com"),
                org.mockito.ArgumentMatchers.eq(UserIdentity.IdentityType.LOCAL_PASSWORD)))
                .thenReturn(Optional.of(user));
        when(identityService.findByEmail(org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.eq("bob@example.com")))
                .thenReturn(Optional.of(user));

        ApiResponse<UserProfileResponse> resp = controller.me();

        assertThat(resp.success()).isTrue();
        assertThat(resp.data().displayName()).isEqualTo("Bob");
        com.company.platform.jpa.TenantContext.clear();
    }

    private void setId(Object target, UUID id) {
        try {
            java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
