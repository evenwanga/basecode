package com.company.usercenter.config;

import com.company.usercenter.identity.store.EphemeralKeyValueStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 实现的短期键值存储。
 */
public class RedisKeyValueStore implements EphemeralKeyValueStore {

    private final StringRedisTemplate template;

    public RedisKeyValueStore(StringRedisTemplate template) {
        this.template = template;
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        template.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(template.opsForValue().get(key));
    }

    @Override
    public void delete(String key) {
        template.delete(key);
    }
}
