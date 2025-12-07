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

        when(identityService.findIdentity("test@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.of(identity));
        when(identityService.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("test@example.com");

        assertThat(details.getUsername()).isEqualTo("test@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserShouldFailWhenIdentityMissing() {
        when(identityService.findIdentity("missing@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
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

        when(identityService.findIdentity("disable@example.com", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.of(identity));
        when(identityService.findByEmail("disable@example.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("disable@example.com");
        assertThat(details.isEnabled()).isFalse();
    }
}
