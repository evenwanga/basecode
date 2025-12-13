package com.company.usercenter.auth;

import com.company.usercenter.identity.IdentityService;
import com.company.usercenter.identity.User;
import com.company.usercenter.identity.UserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainUserDetailsServiceTest {

    @Mock
    IdentityService identityService;

    DomainUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new DomainUserDetailsService(identityService);
    }

    @Test
    void loadUserSuccess() {
        UserIdentity identity = new UserIdentity();
        identity.setUserId(UUID.randomUUID());
        identity.setIdentifier("test@example.com");
        identity.setType(UserIdentity.IdentityType.LOCAL_PASSWORD);
        identity.setSecret("encoded");

        User user = new User();
        user.setDisplayName("测试");
        user.setPrimaryEmail("test@example.com");
        user.setStatus("ACTIVE");

        // Set Tenant Context
        UUID tenantId = UUID.randomUUID();
        com.company.platform.jpa.TenantContext.setTenantId(tenantId.toString());

        when(identityService.findIdentity(tenantId, "test@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.of(identity));
        when(identityService.findById(identity.getUserId())).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("test@example.com");

        assertThat(details.getUsername()).isEqualTo("test@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.isEnabled()).isTrue();

        com.company.platform.jpa.TenantContext.clear();
    }

    @Test
    void loadUserShouldFailWhenIdentityMissing() {
        UUID tenantId = UUID.randomUUID();
        com.company.platform.jpa.TenantContext.setTenantId(tenantId.toString());

        when(identityService.findIdentity(tenantId, "missing@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);

        com.company.platform.jpa.TenantContext.clear();
    }

    @Test
    void loadUserShouldFailWhenUserDisabled() {
        UserIdentity identity = new UserIdentity();
        identity.setUserId(UUID.randomUUID());
        identity.setIdentifier("disable@example.com");
        identity.setType(UserIdentity.IdentityType.LOCAL_PASSWORD);
        identity.setSecret("encoded");

        User user = new User();
        user.setDisplayName("测试");
        user.setPrimaryEmail("disable@example.com");
        user.setStatus("DISABLED");

        UUID tenantId = UUID.randomUUID();
        com.company.platform.jpa.TenantContext.setTenantId(tenantId.toString());

        when(identityService.findIdentity(tenantId, "disable@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.of(identity));
        when(identityService.findById(identity.getUserId())).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("disable@example.com");
        assertThat(details.isEnabled()).isFalse();

        com.company.platform.jpa.TenantContext.clear();
    }
}
