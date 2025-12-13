package com.company.usercenter.identity;

import com.company.platform.common.ApiResponse;
import com.company.usercenter.api.dto.UserProfileResponse;
import com.company.usercenter.identity.service.VerificationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import io.swagger.v3.oas.annotations.enums.ParameterIn;

// ... existing imports

@RestController
@RequestMapping("/api/identities")
@Tag(name = "身份管理", description = "用户身份（Identity）相关操作。用户（User）在当前架构中是租户隔离的实体，注册和查询均需指定租户上下文。")
public class IdentityController {

        private final IdentityService identityService;
        private final VerificationCodeService verificationCodeService;

        public IdentityController(IdentityService identityService,
                        VerificationCodeService verificationCodeService) {
                this.identityService = identityService;
                this.verificationCodeService = verificationCodeService;
        }

        @Operation(summary = "用户注册", description = "在当前租户下注册新用户账号。需要通过 X-Tenant-Id 头指定目标租户。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "注册成功，返回用户信息"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "邮箱已被注册")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @PostMapping("/register")
        public ApiResponse<User> register(
                        @Parameter(description = "用户注册请求体", required = true) @Valid @RequestBody RegisterRequest request) {
                java.util.UUID tenantId = java.util.UUID
                                .fromString(com.company.platform.jpa.TenantContext.requireTenantId());
                User created = identityService.registerUser(tenantId, request.displayName(), request.email(),
                                request.phone(), request.password());
                return ApiResponse.created(created);
        }

        @Operation(summary = "根据邮箱查询用户", description = "查询当前租户下指定邮箱的用户信息。需要通过 X-Tenant-Id 头指定租户。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回用户信息"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "未找到对应用户")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @GetMapping
        public ApiResponse<User> findByEmail(
                        @Parameter(description = "用户邮箱地址", required = true, example = "user@example.com") @RequestParam @Email String email) {
                java.util.UUID tenantId = java.util.UUID
                                .fromString(com.company.platform.jpa.TenantContext.requireTenantId());
                Optional<User> user = identityService.findByEmail(tenantId, email);
                return user.map(ApiResponse::ok)
                                .orElseGet(() -> ApiResponse.error("未找到用户",
                                                org.springframework.http.HttpStatus.NOT_FOUND));
        }

        @Operation(summary = "获取当前登录用户信息", description = "获取当前已登录用户的个人资料信息。依赖 X-Tenant-Id 头确认当前操作的租户上下文。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功，返回用户资料"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未登录或登录已过期")
        })
        @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "租户ID", required = true, schema = @Schema(type = "string"))
        @GetMapping("/me")
        public ApiResponse<UserProfileResponse> me() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) {
                        return ApiResponse.error("未登录", org.springframework.http.HttpStatus.UNAUTHORIZED);
                }
                String principal = auth.getName();
                java.util.UUID tenantId = java.util.UUID
                                .fromString(com.company.platform.jpa.TenantContext.requireTenantId());

                Optional<User> user = identityService
                                .findByIdentifier(tenantId, principal, UserIdentity.IdentityType.LOCAL_PASSWORD)
                                .or(() -> identityService.findByEmail(tenantId, principal));
                return user
                                .map(u -> ApiResponse.ok(new UserProfileResponse(u.getId(), u.getDisplayName(),
                                                u.getPrimaryEmail(), u.getStatus())))
                                .orElseGet(() -> ApiResponse.error("用户不存在",
                                                org.springframework.http.HttpStatus.UNAUTHORIZED));
        }

        @Operation(summary = "发送验证码", description = "向指定邮箱发送登录验证码。验证码有效期为 5 分钟，长度为 6 位数字。开发阶段为方便测试，验证码会直接在响应中返回；生产环境应通过邮件服务发送。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发送成功，返回验证码（仅开发环境）"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "邮箱格式不正确"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "请求过于频繁，请稍后再试")
        })
        @PostMapping("/otp/send")
        public ApiResponse<String> sendOtp(
                        @Parameter(description = "发送验证码请求体", required = true) @Valid @RequestBody SendOtpRequest request) {
                String code = verificationCodeService.issueCode(request.email(), "login", 6,
                                java.time.Duration.ofMinutes(5));
                return ApiResponse.ok(code);
        }

        @Operation(summary = "校验验证码", description = "校验用户输入的验证码是否正确。验证成功后，该验证码会被立即清理，不可重复使用。")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "验证成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "验证码错误或已过期")
        })
        @PostMapping("/otp/verify")
        public ApiResponse<Boolean> verifyOtp(
                        @Parameter(description = "校验验证码请求体", required = true) @Valid @RequestBody VerifyOtpRequest request) {
                boolean passed = verificationCodeService.verify(request.email(), "login", request.code());
                return passed ? ApiResponse.ok(true)
                                : ApiResponse.error("验证码错误或已过期",
                                                org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        @Schema(description = "用户注册请求")
        public record RegisterRequest(
                        @Schema(description = "用户显示名称", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String displayName,
                        @Schema(description = "用户邮箱，用于登录和接收通知", example = "zhangsan@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @Email String email,
                        @Schema(description = "手机号码（可选）", example = "13800138000") @Size(max = 50) String phone,
                        @Schema(description = "登录密码，建议8位以上包含字母数字", example = "Password123", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String password) {
        }

        @Schema(description = "发送验证码请求")
        public record SendOtpRequest(
                        @Schema(description = "接收验证码的邮箱地址", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @Email String email) {
        }

        @Schema(description = "校验验证码请求")
        public record VerifyOtpRequest(
                        @Schema(description = "接收验证码的邮箱地址", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @Email String email,
                        @Schema(description = "6位数字验证码", example = "123456", minLength = 4, maxLength = 10, requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(min = 4, max = 10) String code) {
        }
}
