package com.company.usercenter.identity.service;

import com.company.usercenter.identity.store.EphemeralKeyValueStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * State/Nonce 临时令牌存储，用于防重与回调校验。
 */
@Service
public class StateTokenService {

    private final EphemeralKeyValueStore store;

    public StateTokenService(EphemeralKeyValueStore store) {
        this.store = store;
    }

    /**
     * 保存 state/nonce，值统一存为 "1"，仅用作存在性校验。
     */
    public void save(String category, String token, Duration ttl) {
        store.put(buildKey(category, token), "1", ttl);
    }

    /**
     * 校验并消费 state/nonce，成功返回 true。
     */
    public boolean consume(String category, String token) {
        String key = buildKey(category, token);
        return store.get(key)
                .map(v -> {
                    store.delete(key);
                    return true;
                })
                .orElse(false);
    }

    private String buildKey(String category, String token) {
        return "state:" + category.toLowerCase(Locale.ROOT) + ":" + token;
    }
}
