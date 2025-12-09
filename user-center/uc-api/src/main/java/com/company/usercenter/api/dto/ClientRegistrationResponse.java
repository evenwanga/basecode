package com.company.usercenter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "OAuth2 客户端注册响应（出于安全考虑，不返回客户端密钥，请在注册时妥善保存）")
public record ClientRegistrationResponse(
        @Schema(description = "客户端内部 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "客户端标识符", example = "my-spa-app")
        String clientId,
        @Schema(description = "已注册的授权回调地址列表", example = "[\"http://localhost:3000/callback\"]")
        List<String> redirectUris,
        @Schema(description = "客户端允许请求的权限范围", example = "[\"openid\", \"profile\", \"email\"]")
        List<String> scopes
) {
}
