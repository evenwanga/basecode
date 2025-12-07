package com.company.usercenter.identity;

import com.company.platform.common.ApiResponse;
import com.company.usercenter.api.dto.UserProfileResponse;
import com.company.usercenter.identity.service.VerificationCodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

@RestController
@RequestMapping("/api/identities")
public class IdentityController {

    private final IdentityService identityService;
    private final VerificationCodeService verificationCodeService;

    public IdentityController(IdentityService identityService,
                              VerificationCodeService verificationCodeService) {
        this.identityService = identityService;
        this.verificationCodeService = verificationCodeService;
    }

    /** 注册本地账号（邮箱 + 密码）。 */
    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequest request) {
        User created = identityService.registerUser(request.displayName(), request.email(), request.phone(), request.password());
        return ApiResponse.created(created);
    }

    /** 根据邮箱查询用户。 */
    @GetMapping
    public ApiResponse<User> findByEmail(@RequestParam @Email String email) {
        Optional<User> user = identityService.findByEmail(email);
        return user.map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.error("未找到用户", org.springframework.http.HttpStatus.NOT_FOUND));
    }

    /** 获取当前登录用户信息。 */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ApiResponse.error("未登录", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        String principal = auth.getName();
        Optional<User> user = identityService.findByIdentifier(principal, UserIdentity.IdentityType.LOCAL_PASSWORD)
                .or(() -> identityService.findByEmail(principal));
        return user
                .map(u -> ApiResponse.ok(new UserProfileResponse(u.getId(), u.getDisplayName(), u.getPrimaryEmail(), u.getStatus())))
                .orElseGet(() -> ApiResponse.error("用户不存在", org.springframework.http.HttpStatus.UNAUTHORIZED));
    }

    /** 下发登录验证码（开发阶段返回验证码，生产应通过邮箱/短信发送）。 */
    @PostMapping("/otp/send")
    public ApiResponse<String> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String code = verificationCodeService.issueCode(request.email(), "login", 6,
                java.time.Duration.ofMinutes(5));
        return ApiResponse.ok(code);
    }

    /** 校验登录验证码，成功则返回 true（会顺便清理已使用的验证码）。 */
    @PostMapping("/otp/verify")
    public ApiResponse<Boolean> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean passed = verificationCodeService.verify(request.email(), "login", request.code());
        return passed ? ApiResponse.ok(true) : ApiResponse.error("验证码错误或已过期",
                org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    public record RegisterRequest(@NotBlank String displayName,
                                  @Email String email,
                                  @Size(max = 50) String phone,
                                  @NotBlank String password) { }

    public record SendOtpRequest(@Email String email) { }

    public record VerifyOtpRequest(@Email String email,
                                   @NotBlank @Size(min = 4, max = 10) String code) { }
}
