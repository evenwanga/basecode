package com.company.usercenter.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(UserRepository userRepository,
                           UserIdentityRepository userIdentityRepository,
                           MembershipRepository membershipRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String displayName, String email, String rawPassword) {
        // 注册前先做唯一性校验，避免命中数据库唯一约束返回 500
        if (email != null && userRepository.findByPrimaryEmail(email).isPresent()) {
            throw new IllegalArgumentException("邮箱已被占用");
        }
        if (email != null && userIdentityRepository.findByIdentifierAndType(email, UserIdentity.IdentityType.LOCAL_PASSWORD).isPresent()) {
            throw new IllegalArgumentException("账号已存在");
        }

        User user = new User();
        user.setDisplayName(displayName);
        user.setPrimaryEmail(email);
        user.setStatus("ACTIVE");
        User saved = userRepository.save(user);

        UserIdentity identity = new UserIdentity();
        identity.setUserId(saved.getId());
        identity.setIdentifier(email);
        identity.setType(UserIdentity.IdentityType.LOCAL_PASSWORD);
        identity.setSecret(passwordEncoder.encode(rawPassword));
        userIdentityRepository.save(identity);
        return saved;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByPrimaryEmail(email);
    }

    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
    }

    public Optional<UserIdentity> findIdentity(String identifier, UserIdentity.IdentityType type) {
        return userIdentityRepository.findByIdentifierAndType(identifier, type);
    }

    public void addMembership(UUID userId, UUID tenantId, UUID orgUnitId, String roles) {
        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setTenantId(tenantId);
        membership.setOrgUnitId(orgUnitId);
        membership.setRoles(roles);
        membershipRepository.save(membership);
    }

    @Transactional
    public void bindRole(UUID membershipId, String roles) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("成员关系不存在"));
        membership.setRoles(roles);
        membershipRepository.save(membership);
    }
}
