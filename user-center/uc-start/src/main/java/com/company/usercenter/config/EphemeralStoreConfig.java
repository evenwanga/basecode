package com.company.usercenter.config;

import com.company.usercenter.identity.store.EphemeralKeyValueStore;
import com.company.usercenter.identity.store.InMemoryKeyValueStore;
import com.company.usercenter.identity.store.ResilientKeyValueStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 短期存储 Bean 配置：默认使用 Redis，失败时自动降级至内存。
 */
@Configuration
public class EphemeralStoreConfig {

    @Bean
    @ConditionalOnMissingBean
    public InMemoryKeyValueStore inMemoryKeyValueStore() {
        return new InMemoryKeyValueStore();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisKeyValueStore redisKeyValueStore(StringRedisTemplate template) {
        return new RedisKeyValueStore(template);
    }

    @Bean
    @Primary
    public EphemeralKeyValueStore ephemeralKeyValueStore(InMemoryKeyValueStore fallback,
                                                         org.springframework.beans.factory.ObjectProvider<RedisKeyValueStore> redisStore) {
        RedisKeyValueStore primary = redisStore.getIfAvailable();
        if (primary != null) {
            return new ResilientKeyValueStore(primary, fallback);
        }
        return fallback;
    }
}
