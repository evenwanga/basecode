package com.company.usercenter.identity.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存实现，作为无 Redis 或 Redis 不可用时的降级方案。
 */
public class InMemoryKeyValueStore implements EphemeralKeyValueStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryKeyValueStore.class);

    private static final class Entry {
        final String value;
        final Instant expireAt;

        Entry(String value, Instant expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value, Duration ttl) {
        Instant expireAt = Instant.now().plus(ttl);
        store.put(key, new Entry(value, expireAt));
    }

    @Override
    public Optional<String> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expireAt.isBefore(Instant.now())) {
            store.remove(key);
            log.debug("内存短期存储键过期已清理: {}", key);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.value);
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }
}
