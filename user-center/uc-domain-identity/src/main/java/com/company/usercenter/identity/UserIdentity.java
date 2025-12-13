package com.company.usercenter.identity;

import com.company.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "user_identities")
public class UserIdentity extends BaseEntity {

    public enum IdentityType {
        LOCAL_PASSWORD,
        EMAIL_OTP,
        PHONE_OTP,
        EXTERNAL_OIDC
    }

    @NotNull
    @Column(nullable = false)
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdentityType type;

    @NotBlank
    @Column(nullable = false)
    private String identifier;

    @Column
    private String secret;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public IdentityType getType() {
        return type;
    }

    public void setType(IdentityType type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
