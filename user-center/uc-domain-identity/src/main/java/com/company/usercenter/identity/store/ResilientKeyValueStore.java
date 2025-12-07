package com.company.usercenter.identity.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * 首选主存（如 Redis），失败时自动降级到备用存储，确保功能可用。
 */
public class ResilientKeyValueStore implements EphemeralKeyValueStore {

    private static final Logger log = LoggerFactory.getLogger(ResilientKeyValueStore.class);

    private final EphemeralKeyValueStore primary;
    private final EphemeralKeyValueStore fallback;

    public ResilientKeyValueStore(EphemeralKeyValueStore primary, EphemeralKeyValueStore fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        try {
            primary.put(key, value, ttl);
            return;
        } catch (Exception ex) {
            log.warn("主存写入失败，回退到备用存储: {}", ex.getMessage());
        }
        fallback.put(key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        try {
            Optional<String> val = primary.get(key);
            if (val.isPresent()) {
                return val;
            }
        } catch (Exception ex) {
            log.warn("主存读取失败，回退到备用存储: {}", ex.getMessage());
        }
        return fallback.get(key);
    }

    @Override
    public void delete(String key) {
        try {
            primary.delete(key);
            return;
        } catch (Exception ex) {
            log.warn("主存删除失败，回退到备用存储: {}", ex.getMessage());
        }
        fallback.delete(key);
    }
}
