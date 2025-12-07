package com.company.usercenter.api.dto;

import java.util.UUID;

/** 当前用户信息响应 DTO。 */
public record UserProfileResponse(
        UUID id,
        String displayName,
        String primaryEmail,
        String status
) {
}
