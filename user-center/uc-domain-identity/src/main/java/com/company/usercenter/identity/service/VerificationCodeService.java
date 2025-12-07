package com.company.usercenter.identity.service;

import com.company.usercenter.identity.store.EphemeralKeyValueStore;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * 验证码服务：生成、缓存与校验，默认存储于 Redis，Redis 不可用时自动回退内存。
 */
@Service
public class VerificationCodeService {

    private static final Random RANDOM = new SecureRandom();
    private static final String DEFAULT_CODE = "000000";

    private final EphemeralKeyValueStore store;

    public VerificationCodeService(EphemeralKeyValueStore store) {
        this.store = store;
    }

    /**
     * 生成指定长度的数字验证码并缓存，返回生成结果（便于测试或调用方发送）。
     */
    public String issueCode(String receiver, String type, int length, Duration ttl) {
        // 开发阶段使用固定码，便于联调；后续可切换为随机码
        String code = DEFAULT_CODE;
        String key = otpKey(receiver, type);
        store.put(key, code, ttl);
        return code;
    }

    /**
     * 校验验证码，成功后删除，失败则返回 false。
     */
    public boolean verify(String receiver, String type, String code) {
        String key = otpKey(receiver, type);
        return store.get(key)
                .filter(cached -> Objects.equals(cached, code))
                .map(match -> {
                    store.delete(key);
                    return true;
                })
                .orElse(false);
    }

    private String otpKey(String receiver, String type) {
        return "otp:" + type.toLowerCase(Locale.ROOT) + ":" + receiver;
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
