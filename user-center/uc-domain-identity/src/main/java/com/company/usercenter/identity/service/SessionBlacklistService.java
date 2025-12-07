package com.company.usercenter.identity.service;

import com.company.usercenter.identity.store.EphemeralKeyValueStore;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 会话黑名单服务，可用于注销 token 后的快速校验。
 */
@Service
public class SessionBlacklistService {

    private final EphemeralKeyValueStore store;

    public SessionBlacklistService(EphemeralKeyValueStore store) {
        this.store = store;
    }

    /**
     * 将 token 的唯一标识（如 jti）加入黑名单。
     */
    public void blacklist(String tokenId, Duration ttl) {
        store.put(blacklistKey(tokenId), "1", ttl);
    }

    /**
     * 判断是否在黑名单中。
     */
    public boolean isBlacklisted(String tokenId) {
        return store.get(blacklistKey(tokenId)).isPresent();
    }

    private String blacklistKey(String tokenId) {
        return "blacklist:" + tokenId;
    }
}
