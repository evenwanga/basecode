package com.company.usercenter.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    List<UserIdentity> findByUserId(UUID userId);

    List<UserIdentity> findByIdentifierAndType(String identifier, UserIdentity.IdentityType type);

    Optional<UserIdentity> findByTenantIdAndIdentifierAndType(UUID tenantId, String identifier,
            UserIdentity.IdentityType type);
}
