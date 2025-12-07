package com.company.usercenter.api.dto;

import java.util.List;
import java.util.UUID;

/** 客户端注册响应 DTO。 */
public record ClientRegistrationResponse(
        UUID id,
        String clientId,
        String clientSecret,
        List<String> redirectUris,
        List<String> scopes
) {
}
