package com.company.usercenter.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    TenantRepository tenantRepository;
    @Mock
    OrganizationUnitRepository organizationUnitRepository;

    TenantService service;

    @BeforeEach
    void setUp() {
        service = new TenantService(tenantRepository, organizationUnitRepository);
    }

    @Test
    void createTenantShouldFailWhenCodeDuplicated() {
        when(tenantRepository.findByCode("dup")).thenReturn(Optional.of(new Tenant()));

        assertThatThrownBy(() -> service.createTenant("dup", "租户"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("租户编码已存在");
    }

    @Test
    void createTenantShouldCreateRootOrg() {
        when(tenantRepository.findByCode("t1")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant t = invocation.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });

        service.createTenant("t1", "租户1");

        ArgumentCaptor<OrganizationUnit> orgCaptor = ArgumentCaptor.forClass(OrganizationUnit.class);
        verify(organizationUnitRepository).save(orgCaptor.capture());
        assertThat(orgCaptor.getValue().getTenantId()).isNotNull();
        assertThat(orgCaptor.getValue().getName()).isEqualTo("Root");
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
