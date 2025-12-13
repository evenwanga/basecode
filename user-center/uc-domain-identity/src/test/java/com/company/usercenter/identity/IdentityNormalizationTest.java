package com.company.usercenter.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("身份归一化测试")
class IdentityNormalizationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository; // Needed for service but not used in this test directly

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private IdentityService service;

    @Test
    @DisplayName("当手机号未归档时，应创建新自然人并关联")
    void shouldCreatePersonWhenMobileNotExists() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        String mobile = "13800138000";

        when(personRepository.findByVerifiedMobile(mobile)).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        // Act
        service.linkPerson(user, mobile);

        // Assert
        assertThat(user.getPersonId()).isNotNull();
        verify(personRepository).save(any(Person.class));

    }

    @Test
    @DisplayName("当手机号已归档时，应直接关联现有自然人")
    void shouldLinkToExistingPersonWhenMobileExists() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        String mobile = "13800138000";
        Person existingPerson = new Person();
        existingPerson.setId(UUID.randomUUID());
        existingPerson.setVerifiedMobile(mobile);

        when(personRepository.findByVerifiedMobile(mobile)).thenReturn(Optional.of(existingPerson));

        // Act
        service.linkPerson(user, mobile);

        // Assert
        assertThat(user.getPersonId()).isEqualTo(existingPerson.getId());
        verify(personRepository, never()).save(any(Person.class));

    }
}
