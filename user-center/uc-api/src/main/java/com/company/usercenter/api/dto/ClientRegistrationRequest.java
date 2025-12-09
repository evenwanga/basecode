package com.company.usercenter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "OAuth2 客户端注册请求")
public record ClientRegistrationRequest(
        @Schema(description = "客户端标识符，全局唯一，用于 OAuth2 授权流程中标识客户端", example = "my-spa-app", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String clientId,
        @Schema(description = "客户端密钥，用于客户端身份验证。请妥善保管，系统不会再次显示此密钥", example = "super-secret-key", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String clientSecret,
        @Schema(description = "授权回调地址列表，OAuth2 授权完成后用户将被重定向到此地址。必须与实际地址完全匹配", example = "[\"http://localhost:3000/callback\", \"https://myapp.com/oauth/callback\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty List<String> redirectUris,
        @Schema(description = "请求的权限范围列表，如 openid、profile、email 等", example = "[\"openid\", \"profile\", \"email\"]")
        List<String> scopes
) {
}
