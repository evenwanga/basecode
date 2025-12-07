package com.company.usercenter.auth;

import com.company.usercenter.identity.IdentityService;
import com.company.usercenter.identity.User;
import com.company.usercenter.identity.UserIdentity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Objects;

/**
 * 基于领域用户的 UserDetailsService，支持本地密码登录。
 */
public class DomainUserDetailsService implements UserDetailsService {

    private final IdentityService identityService;

    public DomainUserDetailsService(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserIdentity identity = identityService.findIdentity(username, UserIdentity.IdentityType.LOCAL_PASSWORD)
                .orElseThrow(() -> new UsernameNotFoundException("账号不存在"));
        if (identity.getSecret() == null) {
            throw new UsernameNotFoundException("账号未设置密码");
        }
        User user = identityService.findById(identity.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        boolean enabled = "ACTIVE".equalsIgnoreCase(user.getStatus());
        return org.springframework.security.core.userdetails.User.withUsername(identity.getIdentifier())
                .password(Objects.requireNonNull(identity.getSecret()))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .disabled(!enabled)
                .build();
    }
}
