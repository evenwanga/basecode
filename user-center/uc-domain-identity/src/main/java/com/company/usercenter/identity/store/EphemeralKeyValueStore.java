package com.company.usercenter.identity.store;

import java.time.Duration;
import java.util.Optional;

/**
 * 短期键值存储接口，用于验证码、state/nonce、会话黑名单等短生命周期数据。
 */
public interface EphemeralKeyValueStore {

    /**
     * 写入带过期时间的键值。
     */
    void put(String key, String value, Duration ttl);

    /**
     * 读取值（若已过期则返回空并清理）。
     */
    Optional<String> get(String key);

    /**
     * 删除键。
     */
    void delete(String key);
}
