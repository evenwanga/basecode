package com.company.usercenter.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPrimaryEmail(String email);
    Optional<User> findByPrimaryPhone(String phone);
}
