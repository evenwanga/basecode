package com.company.usercenter.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    Optional<Person> findByVerifiedMobile(String verifiedMobile);

    Optional<Person> findByIdCard(String idCard);
}
