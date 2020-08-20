package com.github.zomin.tool.service;

import com.github.zomin.cache.Cache;
import com.github.zomin.cache.LayeringCache;
import com.github.zomin.cache.caffeine.CaffeineCache;
import com.github.zomin.manager.AbstractCacheManager;
import com.github.zomin.setting.FirstCacheSetting;
import com.github.zomin.setting.LayeringCacheSetting;
import com.github.zomin.setting.SecondaryCacheSetting;
import com.github.zomin.stats.StatsService;
import com.github.zomin.util.BeanFactory;
import com.github.zomin.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * 操作缓存的服务
 *
 * @author yuhao.wang3
 */
public class CacheService {
    /**
     * 删除缓存
     *
     * @param cacheName   缓存名称
     * @param internalKey 内部缓存名，由[一级缓存有效时间-二级缓存有效时间-二级缓存自动刷新时间]组成
     * @param key         key，可以为NULL，如果是NULL则清空缓存
     */
    public void deleteCache(String cacheName, String internalKey, String key) {
        if (StringUtils.isBlank(cacheName) || StringUtils.isBlank(internalKey)) {
            return;
        }
        LayeringCacheSetting defaultSetting = new LayeringCacheSetting(new FirstCacheSetting(), new SecondaryCacheSetting(), "默认缓存配置（删除时生成）");
        Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
        if (StringUtils.isBlank(key)) {
            // 清空缓存
            for (AbstractCacheManager cacheManager : cacheManagers) {
                // 删除缓存统计信息
                String redisKey = StatsService.CACHE_STATS_KEY_PREFIX + cacheName + internalKey;
                if(cacheManager.getStats()) {
                    BeanFactory.getBean(StatsService.class).resetCacheStat(redisKey);
                }
                // 删除缓存
                Collection<Cache> caches = cacheManager.getCache(cacheName);
                if (CollectionUtils.isEmpty(caches)) {
                    // 如果没有找到Cache就新建一个默认的
                    Cache cache = cacheManager.getCache(cacheName, defaultSetting);
                    cache.clear();
                    if(cacheManager.getStats()) {
                        // 删除统计信息
                        redisKey = StatsService.CACHE_STATS_KEY_PREFIX + cacheName + defaultSetting.getInternalKey();
                        BeanFactory.getBean(StatsService.class).resetCacheStat(redisKey);
                    }
                } else {
                    for (Cache cache : caches) {
                        cache.clear();
                    }
                }
            }

            return;
        }

        // 删除指定key
        for (AbstractCacheManager cacheManager : cacheManagers) {
            Collection<Cache> caches = cacheManager.getCache(cacheName);
            if (CollectionUtils.isEmpty(caches)) {
                // 如果没有找到Cache就新建一个默认的
                Cache cache = cacheManager.getCache(cacheName, defaultSetting);
                cache.evict(key);
            } else {
                for (Cache cache : caches) {
                    cache.evict(key);
                }
            }

        }
    }


    /**
     * 查询所有本地缓存
     *
     * @return HashMap<String,Object> 集合
     */
    public HashMap<String,Object> findAllCache(String cacheName) {
        HashMap<String,Object> cacheMap = new HashMap<>(16);
        if(StringUtils.isNotBlank(cacheName)) {
            Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
            for (AbstractCacheManager cacheManager : cacheManagers) {
                Collection<Cache> caches = cacheManager.getCache(cacheName);
                if(!CollectionUtils.isEmpty(caches)) {
                    for (Cache cache : caches) {
                        Object value = cache.get(cacheName);
                        cacheMap.put(cacheName, value);
                    }
                }
            }
        } else {
            Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
            for (AbstractCacheManager cacheManager : cacheManagers) {
                Set<String> keySet = cacheManager.getCacheContainer().keySet();
                for (String tempKey : keySet) {
                    ConcurrentMap<String,Cache> cacheConcurrentMap = cacheManager.getCacheContainer().get(tempKey);
                    for (String tempCacheKey : cacheConcurrentMap.keySet()){
                        LayeringCache lc = (LayeringCache)cacheConcurrentMap.get(tempCacheKey);
                        ConcurrentMap cm = ((CaffeineCache) lc.getFirstCache()).getNativeCache().asMap();
                        Set<String> cmKeyset = cm.keySet();
                        for (String cmKey : cmKeyset) {
                            Object value = cm.get(cmKey);
                            cacheMap.put(tempKey, value);
                        }
                    }
                }
            }
        }
        return cacheMap;
    }
}
