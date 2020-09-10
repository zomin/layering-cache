package com.github.zomin.cache;

import com.github.zomin.manager.CacheManager;
import com.github.zomin.setting.FirstCacheSetting;
import com.github.zomin.setting.LayeringCacheSetting;
import com.github.zomin.setting.SecondaryCacheSetting;
import com.github.zomin.support.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;

/**
 * 多级缓存操作类
 * Created LayeringCacheUtils by kalend.zhang on 2020/7/2.
 *
 * @author kalend.zhang
 */
@Component
public class LayeringCacheUtils {
    private Logger log = LoggerFactory.getLogger(LayeringCacheUtils.class);

    @Autowired
    private CacheManager layerCacheManager;

    /**
     * 获取一级缓存，如有则返回一级缓存，如无则查询二级缓存，当二级缓存也不存在返回null
     * 这里的value和key都支持SpEL 表达式
     *
     * @param cacheName 缓存名称，支持SpEL表达式
     * @param key 缓存key，支持SpEL表达式
     * @param depict 描述
     * @param firstCacheSetting 一级缓存配置 可以为null
     * @param secondaryCacheSetting 二级缓存配置 可以为null
     * @return {@link Object}
     */
    public Object getCache(String cacheName, String key,
                           String depict, FirstCacheSetting firstCacheSetting,
                           SecondaryCacheSetting secondaryCacheSetting) {
        if(ObjectUtils.isEmpty(firstCacheSetting)){
            firstCacheSetting = new FirstCacheSetting();
        }
        if(ObjectUtils.isEmpty(secondaryCacheSetting)){
            secondaryCacheSetting =new SecondaryCacheSetting();
        }
        LayeringCacheSetting layeringCacheSetting = new LayeringCacheSetting(firstCacheSetting, secondaryCacheSetting, depict);
        try {
            // 执行查询缓存方法
            return executeGet(cacheName, key,layeringCacheSetting);
        } catch (SerializationException e) {
            // 如果是序列化异常需要先删除原有缓存
            String[] cacheNames = new String[]{cacheName};
            // 删除缓存
            delete(cacheNames, key, layeringCacheSetting);
            log.warn(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询缓存方法
     * @param cacheName 缓存名称，支持SpEL表达式
     * @param key 缓存key，支持SpEL表达式
     * @param layeringCacheSetting 缓存配置
     * @return @return {@link Object}
     */
    private Object executeGet(String cacheName, String key,
                              LayeringCacheSetting layeringCacheSetting) {
        // 通过cacheName和缓存配置获取Cache
        Cache cache = layerCacheManager.getCache(cacheName, layeringCacheSetting);
        // 通Cache获取值
        return cache.get(key);
    }


    /**
     * 删除缓存
     *
     * @param cacheNames 缓存名称
     * @param key 缓存key，支持SpEL表达式
     * @param removeAll 是否删除缓存中所有数据
     */
    public void removeCache(String[] cacheNames, String key, Boolean removeAll, String depict,
                            FirstCacheSetting firstCacheSetting,
                            SecondaryCacheSetting secondaryCacheSetting) {
        try {
            LayeringCacheSetting layeringCacheSetting = new LayeringCacheSetting(firstCacheSetting, secondaryCacheSetting, depict);
            // 执行查询缓存方法
            executeRemove(cacheNames,key,removeAll,layeringCacheSetting);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除缓存
     *
     * @param cacheNames 缓存名称
     * @param key 缓存key，支持SpEL表达式
     * @param removeAll 是否删除缓存中所有数据
     */
    private void executeRemove(String[] cacheNames, String key, Boolean removeAll,
                               LayeringCacheSetting layeringCacheSetting) {
        // 判断是否删除所有缓存数据
        if (removeAll) {
            // 删除所有缓存数据（清空）
            for (String cacheName : cacheNames) {
                Collection<Cache> caches = layerCacheManager.getCache(cacheName);
                if (CollectionUtils.isEmpty(caches)) {
                    // 如果没有找到Cache就新建一个默认的
                    Cache cache = layerCacheManager.getCache(cacheName, layeringCacheSetting);
                    cache.clear();
                } else {
                    for (Cache cache : caches) {
                        cache.clear();
                    }
                }
            }
        } else {
            // 删除指定key
            delete(cacheNames, key, layeringCacheSetting);
        }
    }

    /**
     * 删除执行缓存名称上的指定key
     *
     * @param cacheNames 缓存名称
     */
    private void delete(String[] cacheNames, String key, LayeringCacheSetting layeringCacheSetting) {
        for (String cacheName : cacheNames) {
            Collection<Cache> caches = layerCacheManager.getCache(cacheName);
            if (CollectionUtils.isEmpty(caches)) {
                // 如果没有找到Cache就新建一个默认的
                Cache cache = layerCacheManager.getCache(cacheName, layeringCacheSetting);
                cache.evict(key);
            } else {
                for (Cache cache : caches) {
                    cache.evict(key);
                }
            }
        }
    }

    /**
     * 将对应数据放到缓存中，原来有值就直接覆盖
     * @param cacheNames 缓存名称
     * @param key 缓存key，支持SpEL表达式
     * @param result 缓存数据
     * @param depict 描述
     * @param firstCacheSetting 一级缓存配置
     * @param secondaryCacheSetting 二级缓存配置
     * @return {@link Object}
     */
    public Object putCache(String[] cacheNames, String key, Object result,
                           String depict, FirstCacheSetting firstCacheSetting,
                           SecondaryCacheSetting secondaryCacheSetting) {
        if(ObjectUtils.isEmpty(firstCacheSetting)){
            firstCacheSetting = new FirstCacheSetting();
        }
        if(ObjectUtils.isEmpty(secondaryCacheSetting)){
            secondaryCacheSetting =new SecondaryCacheSetting();
        }
        try {
            LayeringCacheSetting layeringCacheSetting = new LayeringCacheSetting(firstCacheSetting, secondaryCacheSetting, depict);
            // 执行查询缓存方法
            return executePut(cacheNames,key,result,layeringCacheSetting);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            throw e;
        }
    }


    /**
     * 将对应key-value放到缓存，如果key原来有值就直接覆盖
     *
     * @return {@link Object}
     */
    private Object executePut(String[] cacheNames, String key, Object result, LayeringCacheSetting layeringCacheSetting) {
        for (String cacheName : cacheNames) {
            // 通过cacheName和缓存配置获取Cache
            Cache cache = layerCacheManager.getCache(cacheName, layeringCacheSetting);
            cache.put(key, result);
        }
        return result;
    }

}
