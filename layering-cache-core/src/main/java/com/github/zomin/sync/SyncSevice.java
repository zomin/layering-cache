package com.github.zomin.sync;

import com.alibaba.fastjson.JSON;
import com.github.zomin.cache.Cache;
import com.github.zomin.cache.LayeringCache;
import com.github.zomin.manager.AbstractCacheManager;
import com.github.zomin.manager.CacheManager;
import com.github.zomin.setting.FirstCacheSetting;
import com.github.zomin.setting.LayeringCacheSetting;
import com.github.zomin.setting.SecondaryCacheSetting;
import com.github.zomin.stats.StatsService;
import com.github.zomin.support.Lock;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created SyncSevice by kalend.zhang on 2020/7/3.
 *
 * @author kalend.zhang
 */

public class SyncSevice {
    private static Logger log = LoggerFactory.getLogger(StatsService.class);
    /**
     * {@link AbstractCacheManager }
     */
    private AbstractCacheManager cacheManager;

    /**
     * 定时任务线程池
     */
    /**
     * 定时任务线程池
     * getExecutor
     *
     * @return java.util.concurrent.ScheduledThreadPoolExecutor
     **/
    public static ScheduledThreadPoolExecutor getExecutor() {
        ThreadFactory threadFactory =
            new ThreadFactoryBuilder().setNameFormat("Scheduled-pool-%d").build();
        return new ScheduledThreadPoolExecutor(50,
                                               threadFactory,
                                               new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 同步缓存 syncCache
     *
     * @param syncCacheNames 指定同步Cache名称
     * @param initialDelay 初始化延时时间
     * @param delay 执行频率
     **/
    public void syncCache(List<String> syncCacheNames, long initialDelay, long delay) {
        RedisTemplate<String, Object> redisTemplate = cacheManager.getRedisTemplate();
        getExecutor().scheduleWithFixedDelay(() -> {
            if(log.isDebugEnabled()) {
                log.debug(Thread.currentThread().getName() + "执行缓存主动刷新定时任务");
            }
            Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
            for (AbstractCacheManager abstractCacheManager : cacheManagers) {
                // 获取CacheManager
                CacheManager cacheManager = abstractCacheManager;
                Collection<String> cacheNames;
                if(CollectionUtils.isEmpty(syncCacheNames)) {
                    cacheNames = cacheManager.getCacheNames();
                } else {
                    cacheNames = syncCacheNames;
                }
                for (String cacheName : cacheNames) {
                    if(log.isDebugEnabled()) {
                        log.debug("cacheName:{}", cacheName);
                    }
                    // 获取Cache
                    Collection<Cache> caches = cacheManager.getCache(cacheName);
                    if(CollectionUtils.isEmpty(caches)) {
                        if(log.isDebugEnabled()) {
                            log.debug("一级缓存为空，根据配置初始化一级缓存");
                        }
                        Object cacheValue = redisTemplate.opsForValue().get(cacheName);
                        if(!ObjectUtils.isEmpty(cacheValue)){
                            LayeringCacheSetting layeringCacheSetting =
                                new LayeringCacheSetting(new FirstCacheSetting(), new SecondaryCacheSetting(), "SYNC");
                            Cache cache = cacheManager.getCache(cacheName, layeringCacheSetting);
                            ((LayeringCache) cache).getFirstCache().put(cacheName, cacheValue);
                            if(log.isDebugEnabled()) {
                                log.debug("查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}", cacheName, JSON.toJSONString(cacheValue));
                            }
                        }
                    } else {
                        for (Cache cache : caches) {
                            String redisKey = cache.getName();
                            LayeringCache layeringCache = (LayeringCache) cache;
                            Lock lock = new Lock(redisTemplate, redisKey, 60, 5000);
                            try {
                                if (lock.tryLock()) {
                                    Object cacheValue = layeringCache.getSecondCache().get(cacheName);
                                    if(ObjectUtils.isEmpty(cacheValue)){
                                        layeringCache.getFirstCache().evict(cacheName);
                                        if(log.isDebugEnabled()) {
                                            log.debug("查询二级缓存值为NULL,并将key={}一级缓存清理", cacheName);
                                        }
                                    } else {
                                        layeringCache.getFirstCache().put(cacheName,cacheValue);
                                        if(log.isDebugEnabled()) {
                                            log.debug("查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}", cacheName,
                                                      JSON.toJSONString(cacheValue)
                                                     );
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            } finally {
                                lock.unlock();
                            }
                        }
                    }

                }
            }
        }, initialDelay, delay, TimeUnit.MINUTES);
    }



    /**
     * 关闭线程池
     */
    public void shutdownExecutor() {
        getExecutor().shutdown();
    }

    public void setCacheManager(AbstractCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}
