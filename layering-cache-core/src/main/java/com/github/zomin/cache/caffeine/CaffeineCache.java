package com.github.zomin.cache.caffeine;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.zomin.cache.AbstractValueAdaptingCache;
import com.github.zomin.setting.FirstCacheSetting;
import com.github.zomin.support.ExpireMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.NullValue;

import java.util.concurrent.Callable;

/**
 * 基于Caffeine实现的一级缓存
 *
 * @author kalend.zhang
 */
public class CaffeineCache extends AbstractValueAdaptingCache {
    protected static final Logger log = LoggerFactory.getLogger(CaffeineCache.class);

    /**
     * 缓存对象
     */
    private final Cache<Object, Object> cache;

    /**
     * 使用name和{@link FirstCacheSetting}创建一个 {@link CaffeineCache} 实例
     *
     * @param name              缓存名称
     * @param firstCacheSetting 一级缓存配置 {@link FirstCacheSetting}
     * @param stats             是否开启统计模式
     */
    public CaffeineCache(String name, FirstCacheSetting firstCacheSetting, boolean stats) {
        super(stats, name);
        this.cache = getCache(firstCacheSetting);
    }

    @Override
    public Cache<Object, Object> getNativeCache() {
        return this.cache;
    }

    @Override
    public Object get(Object key) {
        if(log.isDebugEnabled()) {
            log.debug("caffeine缓存 key={} 获取缓存", JSON.toJSONString(key));
        }

        if (isStats()) {
            getCacheStats().addCacheRequestCount(1);
        }

        if (this.cache instanceof LoadingCache) {
            return ((LoadingCache<Object, Object>) this.cache).get(key);
        }
        return cache.getIfPresent(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        if(log.isDebugEnabled()) {
            log.debug("caffeine缓存 key={} 获取缓存， 如果没有命中就走库加载缓存", JSON.toJSONString(key));
        }

        if (isStats()) {
            getCacheStats().addCacheRequestCount(1);
        }

        Object result = this.cache.get(key, k -> loaderValue(key, valueLoader));
        // 如果不允许存NULL值 直接删除NULL值缓存
        boolean isEvict = !isAllowNullValues() && (result == null || result instanceof org.springframework.cache.support.NullValue);
        if (isEvict) {
            evict(key);
        }
        return (T) fromStoreValue(result);
    }

    @Override
    public void put(Object key, Object value) {
        // 允许存NULL值
        if (isAllowNullValues() && value instanceof org.springframework.cache.support.NullValue) {
            if(log.isDebugEnabled()) {
                log.debug("caffeine缓存 key={} put缓存，缓存值：{}", JSON.toJSONString(key), JSON.toJSONString(value));
            }
            this.cache.put(key, toStoreValue(value));
            return;
        }

        // 不允许存NULL值
        if (value != null ) {
            if(log.isDebugEnabled()) {
                log.debug("caffeine缓存 key={} put缓存，缓存值：{}", JSON.toJSONString(key), JSON.toJSONString(value));
            }
            this.cache.put(key, toStoreValue(value));
        } else {
            if(log.isDebugEnabled()) {
                log.debug("缓存值为NULL并且不允许存NULL值，不缓存数据");
            }
        }
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if(log.isDebugEnabled()) {
            log.debug("caffeine缓存 key={} putIfAbsent 缓存，缓存值：{}", JSON.toJSONString(key), JSON.toJSONString(value));
        }
        boolean flag = !isAllowNullValues() && (value == null || value instanceof NullValue);
        if (flag) {
            return null;
        }
        Object result = this.cache.get(key, k -> toStoreValue(value));
        return fromStoreValue(result);
    }

    @Override
    public void evict(Object key) {
        if(log.isDebugEnabled()) {
            log.debug("caffeine缓存 key={} 清除缓存", JSON.toJSONString(key));
        }
        this.cache.invalidate(key);
    }

    @Override
    public void clear() {
        if(log.isDebugEnabled()) {
            log.debug("caffeine缓存 清空缓存");
        }
        this.cache.invalidateAll();
    }

    /**
     * 加载数据
     */
    private <T> Object loaderValue(Object key, Callable<T> valueLoader) {
        long start = System.currentTimeMillis();
        if (isStats()) {
            getCacheStats().addCachedMethodRequestCount(1);
        }

        try {
            T t = valueLoader.call();
            if(log.isDebugEnabled()) {
                log.debug("caffeine缓存 key={} 从库加载缓存: {}", JSON.toJSONString(key), JSON.toJSONString(t));
            }

            if (isStats()) {
                getCacheStats().addCachedMethodRequestTime(System.currentTimeMillis() - start);
            }
            return toStoreValue(t);
        } catch (Exception e) {
            throw new LoaderCacheValueException(key, e);
        }

    }

    /**
     * 根据配置获取本地缓存对象
     *
     * @param firstCacheSetting 一级缓存配置
     * @return {@link Cache}
     */
    private static Cache<Object, Object> getCache(FirstCacheSetting firstCacheSetting) {
        // 根据配置创建Caffeine builder
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.initialCapacity(firstCacheSetting.getInitialCapacity());
        builder.maximumSize(firstCacheSetting.getMaximumSize());
        if (ExpireMode.WRITE.equals(firstCacheSetting.getExpireMode())) {
            builder.expireAfterWrite(firstCacheSetting.getExpireTime(), firstCacheSetting.getTimeUnit());
        } else if (ExpireMode.ACCESS.equals(firstCacheSetting.getExpireMode())) {
            builder.expireAfterAccess(firstCacheSetting.getExpireTime(), firstCacheSetting.getTimeUnit());
        }
        // 根据Caffeine builder创建 Cache 对象
        return builder.build();
    }

    @Override
    public boolean isAllowNullValues() {
        return false;
    }
}
