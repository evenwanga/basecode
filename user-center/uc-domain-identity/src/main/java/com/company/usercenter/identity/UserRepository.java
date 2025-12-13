package com.company.usercenter.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findByPrimaryEmail(String email);

    List<User> findByPrimaryPhone(String phone);

    Optional<User> findByTenantIdAndPrimaryEmail(UUID tenantId, String email);

    Optional<User> findByTenantIdAndPrimaryPhone(UUID tenantId, String phone);
}
