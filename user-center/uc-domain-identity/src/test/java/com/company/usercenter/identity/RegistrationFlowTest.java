package com.company.usercenter.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("注册流程测试")
class RegistrationFlowTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private IdentityService service;

    @Test
    @DisplayName("新租户注册应成功创建用户并关联自然人")
    void shouldRegisterUserSuccessfullyInNewTenant() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String mobile = "13800000001";
        String email = "test@example.com";
        String dummyPwd = "hashedPwd";

        when(userRepository.findByTenantIdAndPrimaryEmail(tenantId, email)).thenReturn(Optional.empty());
        when(personRepository.findByVerifiedMobile(mobile)).thenReturn(Optional.empty()); // New Person
        when(passwordEncoder.encode(any())).thenReturn(dummyPwd);

        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        // Act
        User registeredUser = service.registerUser(tenantId, "Test User", email, mobile, "password");

        // Assert
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getTenantId()).isEqualTo(tenantId);
        assertThat(registeredUser.getPersonId()).isNotNull();

        verify(userIdentityRepository).save(any(UserIdentity.class));
        verify(userRepository).save(any(User.class));
        verify(personRepository).save(any(Person.class));
    }

    @Test
    @DisplayName("不同租户注册相同手机号应关联已有自然人")
    void shouldLinkToExistingPersonForDifferentTenant() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String mobile = "13800000001";
        String email = "another@example.com";
        Person existingPerson = new Person();
        existingPerson.setId(UUID.randomUUID());
        existingPerson.setVerifiedMobile(mobile);

        when(userRepository.findByTenantIdAndPrimaryEmail(tenantId, email)).thenReturn(Optional.empty());
        when(personRepository.findByVerifiedMobile(mobile)).thenReturn(Optional.of(existingPerson));
        when(passwordEncoder.encode(any())).thenReturn("pwd");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        // Act
        User registeredUser = service.registerUser(tenantId, "User B", email, mobile, "password");

        // Assert
        assertThat(registeredUser.getPersonId()).isEqualTo(existingPerson.getId());
        verify(personRepository, never()).save(any(Person.class)); // Should reuse
    }
}
