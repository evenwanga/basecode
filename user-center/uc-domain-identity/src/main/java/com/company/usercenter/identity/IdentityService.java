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
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            MembershipRepository membershipRepository,
            PersonRepository personRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.membershipRepository = membershipRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册用户 (租户隔离)
     */
    @Transactional
    public User registerUser(UUID tenantId, String displayName, String email, String phone, String rawPassword) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }

        // 1. Check uniqueness within Tenant
        // For brevity, using email checks as primary example. Test uses email/mobile.
        if (email != null && userRepository.findByTenantIdAndPrimaryEmail(tenantId, email).isPresent()) {
            throw new IllegalArgumentException("邮箱已被占用");
        }
        if (phone != null && userRepository.findByTenantIdAndPrimaryPhone(tenantId, phone).isPresent()) {
            throw new IllegalArgumentException("手机号已被占用");
        }

        // 2. Create User
        User user = new User();
        user.setTenantId(tenantId);
        user.setDisplayName(displayName);
        user.setPrimaryEmail(email);
        user.setPrimaryPhone(phone);
        user.setStatus("ACTIVE");
        // user.setPasswordHash(passwordEncoder.encode(rawPassword)); // logic moved to
        // Identity

        // 3. Link Person (Global Identity)
        // Linking logic relies on verified Mobile
        if (phone != null) {
            linkPerson(user, phone);
        }

        User savedUser = userRepository.save(user);

        // 4. Create UserIdentity (Tenant Scoped)
        UserIdentity identity = new UserIdentity();
        identity.setTenantId(tenantId);
        identity.setUserId(savedUser.getId());
        String identifier = (email != null) ? email : phone;
        identity.setIdentifier(identifier);
        identity.setType(UserIdentity.IdentityType.LOCAL_PASSWORD);
        identity.setSecret(passwordEncoder.encode(rawPassword));
        userIdentityRepository.save(identity);

        return savedUser;
    }

    /**
     * 按邮箱查找 (可能存在多个，不同租户)
     */
    public void linkPerson(User user, String mobile) {
        Optional<Person> existingPerson = personRepository.findByVerifiedMobile(mobile);
        if (existingPerson.isPresent()) {
            user.setPersonId(existingPerson.get().getId());
        } else {
            Person newPerson = new Person();
            newPerson.setVerifiedMobile(mobile);
            newPerson.setStatus("ACTIVE"); // Enable by default
            Person savedPerson = personRepository.save(newPerson);
            user.setPersonId(savedPerson.getId());
        }
        // User update is handled by caller (registerUser) or should be here?
        // Method signature doesn't imply return.
        // If caller expects user to have personId set, this modifies the object.
    }

    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
    }

    public Optional<UserIdentity> findIdentity(UUID tenantId, String identifier,
            UserIdentity.IdentityType type) {
        return userIdentityRepository.findByTenantIdAndIdentifierAndType(tenantId, identifier, type);
    }

    /**
     * 查找用户 (通过租户和标识)
     */
    public Optional<User> findByIdentifier(UUID tenantId, String identifier, UserIdentity.IdentityType type) {
        return userIdentityRepository.findByTenantIdAndIdentifierAndType(tenantId, identifier, type)
                .flatMap(identity -> userRepository.findById(identity.getUserId()));
    }

    public Optional<User> findByEmail(UUID tenantId, String email) {
        return userRepository.findByTenantIdAndPrimaryEmail(tenantId, email);
    }

    public boolean hasMembership(UUID userId, UUID tenantId) {
        return membershipRepository.findByUserId(userId)
                .stream()
                .anyMatch(m -> tenantId.equals(m.getTenantId()));
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
