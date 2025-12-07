package com.company.usercenter.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 客户端注册请求 DTO。 */
public record ClientRegistrationRequest(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotEmpty List<String> redirectUris,
        List<String> scopes
) {
}
