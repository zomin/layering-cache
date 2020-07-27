package com.github.zomin.manager;

import com.github.zomin.cache.Cache;
import com.github.zomin.cache.LayeringCache;
import com.github.zomin.cache.caffeine.CaffeineCache;
import com.github.zomin.cache.redis.RedisCache;
import com.github.zomin.setting.LayeringCacheSetting;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author kalend.zhang
 */
public class LayeringCacheManager extends AbstractCacheManager {
    public LayeringCacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        cacheManagers.add(this);
    }

    @Override
    protected Cache getMissingCache(String name, LayeringCacheSetting layeringCacheSetting) {
        // 创建一级缓存
        CaffeineCache caffeineCache = new CaffeineCache(name, layeringCacheSetting.getFirstCacheSetting(), getStats());
        // 创建二级缓存
        RedisCache redisCache = new RedisCache(name, redisTemplate, layeringCacheSetting.getSecondaryCacheSetting(), getStats());
        return new LayeringCache(redisTemplate, caffeineCache, redisCache, super.getStats(), layeringCacheSetting);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
