package com.company.usercenter.api.dto;

import java.util.List;
import java.util.UUID;

/** 客户端注册响应 DTO（不回传明文密钥，调用方自持 request 中的 clientSecret）。 */
public record ClientRegistrationResponse(
        UUID id,
        String clientId,
        List<String> redirectUris,
        List<String> scopes
) {
}
