package com.company.usercenter.identity.service;

import com.company.usercenter.identity.store.InMemoryKeyValueStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证码与短期状态服务的内存实现回退测试，确保无 Redis 时行为正确。
 */
class VerificationCodeServiceTest {

    private final InMemoryKeyValueStore store = new InMemoryKeyValueStore();

    @Test
    void issueAndVerifyOtp() {
        VerificationCodeService service = new VerificationCodeService(store, "123456");
        String code = service.issueCode("user@example.com", "login", 6, Duration.ofSeconds(5));
        assertThat(code).isEqualTo("123456");
        assertThat(service.verify("user@example.com", "login", code)).isTrue();
        assertThat(service.verify("user@example.com", "login", code)).isFalse(); // 已消费
    }

    @Test
    void stateAndBlacklistFlow() throws InterruptedException {
        StateTokenService stateService = new StateTokenService(store);
        SessionBlacklistService blacklistService = new SessionBlacklistService(store);

        stateService.save("oauth", "state-123", Duration.ofMillis(100));
        assertThat(stateService.consume("oauth", "state-123")).isTrue();
        assertThat(stateService.consume("oauth", "state-123")).isFalse(); // 已消费

        blacklistService.blacklist("jti-1", Duration.ofMillis(50));
        assertThat(blacklistService.isBlacklisted("jti-1")).isTrue();
        Thread.sleep(60);
        assertThat(blacklistService.isBlacklisted("jti-1")).isFalse(); // 过期
    }
}
