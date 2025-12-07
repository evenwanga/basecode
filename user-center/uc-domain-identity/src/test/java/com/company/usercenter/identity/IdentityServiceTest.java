package com.company.usercenter.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserIdentityRepository userIdentityRepository;
    @Mock
    MembershipRepository membershipRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    IdentityService service;

    @BeforeEach
    void setUp() {
        service = new IdentityService(userRepository, userIdentityRepository, membershipRepository, passwordEncoder);
    }

    @Test
    void registerShouldFailWhenEmailExists() {
        when(userRepository.findByPrimaryEmail("dup@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> service.registerUser("张三", "dup@example.com", null, "pwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("邮箱已被占用");
    }

    @Test
    void registerShouldFallbackToPhoneIdentifier() {
        when(passwordEncoder.encode("pwd")).thenReturn("encoded");
        when(userRepository.findByPrimaryPhone("13800000000")).thenReturn(Optional.empty());
        when(userIdentityRepository.findByIdentifierAndType("13800000000", UserIdentity.IdentityType.LOCAL_PASSWORD))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            setId(u, UUID.randomUUID());
            return u;
        });

        service.registerUser("李四", null, "13800000000", "pwd");

        ArgumentCaptor<UserIdentity> identityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getIdentifier()).isEqualTo("13800000000");
        assertThat(identityCaptor.getValue().getType()).isEqualTo(UserIdentity.IdentityType.LOCAL_PASSWORD);
    }

    private void setId(Object target, UUID id) {
        try {
            var field = target.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
