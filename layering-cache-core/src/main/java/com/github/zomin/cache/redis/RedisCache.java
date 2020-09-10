package com.github.zomin.cache.redis;

import com.alibaba.fastjson.JSON;
import com.github.zomin.cache.AbstractValueAdaptingCache;
import com.github.zomin.setting.SecondaryCacheSetting;
import com.github.zomin.support.AwaitThreadContainer;
import com.github.zomin.support.Lock;
import com.github.zomin.support.ThreadTaskUtils;
import com.github.zomin.util.RedisHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis实现的二级缓存
 *
 * @author kalend.zhang
 */
public class RedisCache extends AbstractValueAdaptingCache {
    protected static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    /**
     * 刷新缓存重试次数
     */
    private static final int RETRY_COUNT = 20;

    /**
     * 刷新缓存等待时间，单位毫秒
     */
    private static final long WAIT_TIME = 20;

    /**
     * 等待线程容器
     */
    private AwaitThreadContainer container = new AwaitThreadContainer();

    /**
     * redis 客户端
     */
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存有效时间,毫秒
     */
    private long expiration;

    /**
     * 缓存主动在失效前强制刷新缓存的时间
     * 单位：毫秒
     */
    private long preloadTime = 0;

    /**
     * 是否强制刷新（执行被缓存的方法），默认是false
     */
    private boolean forceRefresh = false;

    /**
     * 是否使用缓存名称作为 redis key 前缀
     */
    private boolean usePrefix;

    /**
     * 是否允许为NULL
     */
    private final boolean allowNullValues;

    /**
     * 非空值和null值之间的时间倍率，默认是1。allowNullValue=true才有效
     * <p>
     * 如配置缓存的有效时间是200秒，倍率这设置成10，
     * 那么当缓存value为null时，缓存的有效时间将是20秒，非空时为200秒
     * </p>
     */
    private final int magnification;

    /**
     * @param name                  缓存名称
     * @param redisTemplate         redis客户端 redis 客户端
     * @param secondaryCacheSetting 二级缓存配置{@link SecondaryCacheSetting}
     * @param stats                 是否开启统计模式
     */
    public RedisCache(String name, RedisTemplate<String, Object> redisTemplate, SecondaryCacheSetting secondaryCacheSetting, boolean stats) {

        this(name, redisTemplate, secondaryCacheSetting.getTimeUnit().toMillis(secondaryCacheSetting.getExpiration()),
             secondaryCacheSetting.getTimeUnit().toMillis(secondaryCacheSetting.getPreloadTime()),
             secondaryCacheSetting.isForceRefresh(), secondaryCacheSetting.isUsePrefix(),
             secondaryCacheSetting.isAllowNullValue(), secondaryCacheSetting.getMagnification(), stats);
    }

    /**
     * @param name            缓存名称
     * @param redisTemplate   redis客户端   redis 客户端
     * @param expiration      key的有效时间
     * @param preloadTime     缓存主动在失效前强制刷新缓存的时间
     * @param forceRefresh    是否强制刷新（执行被缓存的方法），默认是false
     * @param usePrefix       是否使用缓存名称作为前缀
     * @param allowNullValues 是否允许存NULL值，模式允许
     * @param magnification   非空值和null值之间的时间倍率
     * @param stats           是否开启统计模式
     */
    public RedisCache(String name, RedisTemplate<String, Object> redisTemplate, long expiration, long preloadTime,
                      boolean forceRefresh, boolean usePrefix, boolean allowNullValues, int magnification, boolean stats) {
        super(stats, name);

        Assert.notNull(redisTemplate, "RedisTemplate 不能为NULL");
        this.redisTemplate = redisTemplate;
        this.expiration = expiration;
        this.preloadTime = preloadTime;
        this.forceRefresh = forceRefresh;
        this.usePrefix = usePrefix;
        this.allowNullValues = allowNullValues;
        this.magnification = magnification;
    }

    @Override
    public RedisTemplate<String, Object> getNativeCache() {
        return this.redisTemplate;
    }

    @Override
    public Object get(Object key) {
        if (isStats()) {
            getCacheStats().addCacheRequestCount(1);
        }

        RedisCacheKey redisCacheKey = getRedisCacheKey(key);
        if(log.isDebugEnabled()) {
            log.debug("redis缓存 key= {} 查询redis缓存", redisCacheKey.getKey());
        }
        return redisTemplate.opsForValue().get(redisCacheKey.getKey());
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (isStats()) {
            getCacheStats().addCacheRequestCount(1);
        }

        RedisCacheKey redisCacheKey = getRedisCacheKey(key);
        if(log.isDebugEnabled()) {
            log.debug("redis缓存 key= {} 查询redis缓存如果没有命中，从数据库获取数据", redisCacheKey.getKey());
        }
        // 先获取缓存，如果有直接返回
        Object result = redisTemplate.opsForValue().get(redisCacheKey.getKey());
        if (result != null || redisTemplate.hasKey(redisCacheKey.getKey())) {
            // 刷新缓存
            refreshCache(redisCacheKey, valueLoader, result);
            return (T) fromStoreValue(result);
        }
        // 执行缓存方法
        return executeCacheMethod(redisCacheKey, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        RedisCacheKey redisCacheKey = getRedisCacheKey(key);
        if(log.isDebugEnabled()) {
            log.debug("redis缓存 key= {} put缓存，缓存值：{}", redisCacheKey.getKey(), JSON.toJSONString(value));
        }
        putValue(redisCacheKey, value);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if(log.isDebugEnabled()) {
            log.debug("redis缓存 key= {} putIfAbsent缓存，缓存值：{}", getRedisCacheKey(key).getKey(), JSON.toJSONString(value));
        }
        Object reult = get(key);
        if (reult != null) {
            return reult;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        RedisCacheKey redisCacheKey = getRedisCacheKey(key);
        log.info("清除redis缓存 key= {} ", redisCacheKey.getKey());
        redisTemplate.delete(redisCacheKey.getKey());
    }

    @Override
    public void clear() {
        // 必须开启了使用缓存名称作为前缀，clear才有效
        if (usePrefix) {
            log.info("清空redis缓存 ，缓存前缀为{}", getName());

            Set<String> keys = RedisHelper.scan(redisTemplate, getName() + "*");
            if (!CollectionUtils.isEmpty(keys)) {
                redisTemplate.delete(keys);
            }
        }
    }


    /**
     * 获取 RedisCacheKey
     *
     * @param key 缓存key
     * @return RedisCacheKey
     */
    public RedisCacheKey getRedisCacheKey(Object key) {
        return new RedisCacheKey(key, redisTemplate.getKeySerializer())
            .cacheName(getName()).usePrefix(usePrefix);
    }

    /**
     * 同一个线程循环5次查询缓存，每次等待20毫秒，如果还是没有数据直接去执行被缓存的方法
     */
    private <T> T executeCacheMethod(RedisCacheKey redisCacheKey, Callable<T> valueLoader) {
        Lock redisLock = new Lock(redisTemplate, redisCacheKey.getKey() + "_sync_lock");
        // 同一个线程循环20次查询缓存，每次等待20毫秒，如果还是没有数据直接去执行被缓存的方法
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                // 先取缓存，如果有直接返回，没有再去做拿锁操作
                Object result = redisTemplate.opsForValue().get(redisCacheKey.getKey());
                if (result != null) {
                    if(log.isDebugEnabled()) {
                        log.debug("redis缓存 key= {} 获取到锁后查询查询缓存命中，不需要执行被缓存的方法", redisCacheKey.getKey());
                    }
                    return (T) fromStoreValue(result);
                }

                // 获取分布式锁去后台查询数据
                if (redisLock.lock()) {
                    T t = loaderAndPutValue(redisCacheKey, valueLoader, true);
                    if(log.isDebugEnabled()) {
                        log.debug("redis缓存 key= {} 从数据库获取数据完毕，唤醒所有等待线程", redisCacheKey.getKey());
                    }
                    // 唤醒线程
                    container.signalAll(redisCacheKey.getKey());
                    return t;
                }
                // 线程等待
                if(log.isDebugEnabled()) {
                    log.debug("redis缓存 key= {} 从数据库获取数据未获取到锁，进入等待状态，等待{}毫秒", redisCacheKey.getKey(), WAIT_TIME);
                }
                container.await(redisCacheKey.getKey(), WAIT_TIME);
            } catch (Exception e) {
                container.signalAll(redisCacheKey.getKey());
                throw new LoaderCacheValueException(redisCacheKey.getKey(), e);
            } finally {
                redisLock.unlock();
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("redis缓存 key={} 等待{}次，共{}毫秒，任未获取到缓存，直接去执行被缓存的方法", redisCacheKey.getKey(), RETRY_COUNT,
                      RETRY_COUNT * WAIT_TIME
                     );
        }
        return loaderAndPutValue(redisCacheKey, valueLoader, true);
    }

    /**
     * 加载并将数据放到redis缓存
     */
    private <T> T loaderAndPutValue(RedisCacheKey key, Callable<T> valueLoader, boolean isLoad) {
        long start = System.currentTimeMillis();
        if (isLoad && isStats()) {
            getCacheStats().addCachedMethodRequestCount(1);
        }

        try {
            // 加载数据
            Object result = putValue(key, valueLoader.call());
            if(log.isDebugEnabled()) {
                log.debug("redis缓存 key={} 执行被缓存的方法，并将其放入缓存, 耗时：{}。数据:{}", key.getKey(), System.currentTimeMillis() - start, JSON.toJSONString(result));
            }
            if (isLoad && isStats()) {
                getCacheStats().addCachedMethodRequestTime(System.currentTimeMillis() - start);
            }
            return (T) fromStoreValue(result);
        } catch (Exception e) {
            throw new LoaderCacheValueException(key.getKey(), e);
        }
    }

    private Object putValue(RedisCacheKey key, Object value) {
        Object result = toStoreValue(value);
        // redis 缓存不允许直接存NULL，如果结果返回NULL需要删除缓存
        if (result == null) {
            redisTemplate.delete(key.getKey());
            return result;
        }
        // 不允许缓存NULL值，删除缓存
        if (!isAllowNullValues() && result instanceof org.springframework.cache.support.NullValue) {
            redisTemplate.delete(key.getKey());
            return result;
        }

        // 允许缓存NULL值
        long expirationTime = this.expiration;

        // 将数据放到缓存
        if(expirationTime<=0) {
            redisTemplate.opsForValue().set(key.getKey(), result);
        } else {
            // 允许缓存NULL值且缓存为值为null时需要重新计算缓存时间
            if (isAllowNullValues() && result instanceof org.springframework.cache.support.NullValue) {
                expirationTime = expirationTime / getMagnification();
            }
            redisTemplate.opsForValue().set(key.getKey(), result, expirationTime, TimeUnit.MILLISECONDS);
        }
        return result;
    }

    /**
     * 刷新缓存数据
     */
    private <T> void refreshCache(RedisCacheKey redisCacheKey, Callable<T> valueLoader, Object result) {
        Long ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        Long preload = preloadTime;
        // 允许缓存NULL值，则自动刷新时间也要除以倍数
        boolean flag = isAllowNullValues() && (result instanceof NullValue || result == null);
        if (flag) {
            preload = preload / getMagnification();
        }
        if (null != ttl && ttl > 0 && TimeUnit.SECONDS.toMillis(ttl) <= preload) {
            // 判断是否需要强制刷新在开启刷新线程
            if (!getForceRefresh()) {
                if(log.isDebugEnabled()) {
                    log.debug("redis缓存 key={} 软刷新缓存模式", redisCacheKey.getKey());
                }
                softRefresh(redisCacheKey);
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("redis缓存 key={} 强刷新缓存模式", redisCacheKey.getKey());
                }
                forceRefresh(redisCacheKey, valueLoader);
            }
        }
    }

    /**
     * 软刷新，直接修改缓存时间
     *
     * @param redisCacheKey {@link RedisCacheKey}
     */
    private void softRefresh(RedisCacheKey redisCacheKey) {
        // 加一个分布式锁，只放一个请求去刷新缓存
        Lock redisLock = new Lock(redisTemplate, redisCacheKey.getKey() + "_lock");
        try {
            if (redisLock.tryLock()) {
                redisTemplate.expire(redisCacheKey.getKey(), this.expiration, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            redisLock.unlock();
        }
    }

    /**
     * 硬刷新（执行被缓存的方法）
     *
     * @param redisCacheKey {@link RedisCacheKey}
     * @param valueLoader   数据加载器
     */
    private <T> void forceRefresh(RedisCacheKey redisCacheKey, Callable<T> valueLoader) {
        // 尽量少的去开启线程，因为线程池是有限的
        ThreadTaskUtils.run(() -> {
            // 加一个分布式锁，只放一个请求去刷新缓存
            Lock redisLock = new Lock(redisTemplate, redisCacheKey.getKey() + "_lock");
            try {
                if (redisLock.lock()) {
                    // 获取锁之后再判断一下过期时间，看是否需要加载数据
                    Long ttl = redisTemplate.getExpire(redisCacheKey.getKey());
                    if (null != ttl && ttl > 0 && TimeUnit.SECONDS.toMillis(ttl) <= preloadTime) {
                        // 加载数据并放到缓存
                        loaderAndPutValue(redisCacheKey, valueLoader, false);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                redisLock.unlock();
            }
        });
    }

    /**
     * 是否强制刷新（执行被缓存的方法），默认是false
     *
     * @return boolean
     */
    private boolean getForceRefresh() {
        return forceRefresh;
    }

    /**
     * 非空值和null值之间的时间倍率，默认是1。
     *
     * @return int
     */
    public int getMagnification() {
        return magnification;
    }

    @Override
    public boolean isAllowNullValues() {
        return allowNullValues;
    }
}
