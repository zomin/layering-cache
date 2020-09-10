package com.github.zomin.manager;

import com.github.zomin.cache.Cache;
import com.github.zomin.listener.RedisMessageListener;
import com.github.zomin.setting.LayeringCacheSetting;
import com.github.zomin.stats.CacheStatsInfo;
import com.github.zomin.stats.StatsService;
import com.github.zomin.sync.SyncSevice;
import com.github.zomin.util.BeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 公共的抽象 {@link CacheManager} 的实现.
 *
 * @author kalend.zhang
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean, DisposableBean, BeanNameAware, SmartLifecycle {
    private Logger logger = LoggerFactory.getLogger(AbstractCacheManager.class);

    /**
     * redis pub/sub 容器
     */
    private final RedisMessageListenerContainer container = new RedisMessageListenerContainer();

    /**
     * redis pub/sub 监听器
     */
    private final RedisMessageListener messageListener = new RedisMessageListener();

    /**
     * 缓存容器
     * 外层key是cache_name
     * 里层key是[一级缓存有效时间-二级缓存有效时间-二级缓存自动刷新时间]
     */
    private final ConcurrentMap<String, ConcurrentMap<String, Cache>> cacheContainer = new ConcurrentHashMap<>(16);

    /**
     * 缓存名称容器
     */
    private volatile Set<String> cacheNames = new LinkedHashSet<>();

    /**
     * CacheManager 容器
     */
    static Set<AbstractCacheManager> cacheManagers = new LinkedHashSet<>();

    /**
     * 是否开启统计
     */
    private boolean stats = false;

    /**
     * 统计数据定时任务时间间隔，默认分钟，初始延迟 1
     */
    private long initialDelay = 1;

    /**
     * 统计数据定时任务时间间隔，默认分钟，频率 1
     */
    private long delay = 1;

    /**
     * 是否开启同步刷新一级缓存
     */
    private boolean sync = false;

    /**
     * 刷新一级缓存任务数据定时任务时间间隔，默认分钟，初始延迟 1
     */
    private long syncInitialDelay = 1;

    /**
     * 刷新一级缓存任务数据定时任务时间间隔，默认分钟，频率 1
     */
    private long syncDelay = 1;

    /**
     * 需要主动刷新的Cache
     */
    private List<String> syncCacheNames;

    /**
     * redis 客户端
     */
    RedisTemplate<String, Object> redisTemplate;

    public static Set<AbstractCacheManager> getCacheManager() {
        return cacheManagers;
    }

    @Override
    public Collection<Cache> getCache(String name) {
        ConcurrentMap<String, Cache> cacheMap = this.cacheContainer.get(name);
        if (CollectionUtils.isEmpty(cacheMap)) {
            return Collections.emptyList();
        }
        return cacheMap.values();
    }

    /**
     * Lazy cache initialization on access
     */
    @Override
    @SuppressWarnings("findbugs:JLM_JSR166_UTILCONCURRENT_MONITORENTER")
    public Cache getCache(String name, LayeringCacheSetting layeringCacheSetting) {
        // 第一次获取缓存Cache，如果有直接返回,如果没有加锁往容器里里面放Cache
        ConcurrentMap<String, Cache> cacheMap = this.cacheContainer.get(name);
        if (!CollectionUtils.isEmpty(cacheMap)) {
            if (cacheMap.size() > 1) {
                logger.warn("缓存名称为 {} 的缓存,存在两个不同的过期时间配置，请一定注意保证缓存的key唯一性，否则会出现缓存过期时间错乱的情况", name);
            }
            Cache cache = cacheMap.get(layeringCacheSetting.getInternalKey());
            if (cache != null) {
                return cache;
            }
        }

        // 第二次获取缓存Cache，加锁往容器里里面放Cache
        synchronized (this.cacheContainer) {
            cacheMap = this.cacheContainer.get(name);
            if (!CollectionUtils.isEmpty(cacheMap)) {
                // 从容器中获取缓存
                Cache cache = cacheMap.get(layeringCacheSetting.getInternalKey());
                if (cache != null) {
                    return cache;
                }
            } else {
                cacheMap = new ConcurrentHashMap<>(16);
                cacheContainer.put(name, cacheMap);
                // 更新缓存名称
                updateCacheNames(name);
                // 创建redis监听
                addMessageListener(name);
            }

            // 新建一个Cache对象
            Cache cache = getMissingCache(name, layeringCacheSetting);
            if (cache != null) {
                // 装饰Cache对象
                cache = decorateCache(cache);
                // 将新的Cache对象放到容器
                cacheMap.put(layeringCacheSetting.getInternalKey(), cache);
                if (cacheMap.size() > 1) {
                    logger.warn("缓存名称为 {} 的缓存,存在两个不同的过期时间配置，请一定注意保证缓存的key唯一性，否则会出现缓存过期时间错乱的情况", name);
                }
            }

            return cache;
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    /**
     * 更新缓存名称容器
     *
     * @param name 需要添加的缓存名称
     */
    private void updateCacheNames(String name) {
        cacheNames.add(name);
    }


    /**
     * 获取Cache对象的装饰示例
     *
     * @param cache 需要添加到CacheManager的Cache实例
     * @return 装饰过后的Cache实例
     */
    protected Cache decorateCache(Cache cache) {
        return cache;
    }

    /**
     * 根据缓存名称在CacheManager中没有找到对应Cache时，通过该方法新建一个对应的Cache实例
     *
     * @param name                 缓存名称
     * @param layeringCacheSetting 缓存配置
     * @return {@link Cache}
     */
    protected abstract Cache getMissingCache(String name, LayeringCacheSetting layeringCacheSetting);

    /**
     * 获取缓存容器
     *
     * @return 返回缓存容器
     */
    public ConcurrentMap<String, ConcurrentMap<String, Cache>> getCacheContainer() {
        return cacheContainer;
    }

    /**
     * 添加消息监听
     *
     * @param name 缓存名称
     */
    protected void addMessageListener(String name) {
        container.addMessageListener(messageListener, new ChannelTopic(name));
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        messageListener.setCacheManager(this);
        container.setConnectionFactory(getRedisTemplate().getConnectionFactory());
        container.afterPropertiesSet();
        messageListener.afterPropertiesSet();

        if (getStats()) {
            BeanFactory.getBean(StatsService.class).setCacheManager(this);
            // 采集缓存命中率数据
            BeanFactory.getBean(StatsService.class).syncCacheStats(initialDelay,delay);
        }
        if (isSync()) {
            BeanFactory.getBean(SyncSevice.class).setCacheManager(this);
            BeanFactory.getBean(SyncSevice.class).syncCache(syncCacheNames,syncInitialDelay, syncDelay);
        }
    }

    @Override
    public List<CacheStatsInfo> listCacheStats(String cacheName) {
        return BeanFactory.getBean(StatsService.class).listCacheStats(cacheName);
    }

    @Override
    public void resetCacheStat() {
        BeanFactory.getBean(StatsService.class).resetCacheStat();
    }

    @Override
    public void setBeanName(String name) {
        container.setBeanName("redisMessageListenerContainer");
    }

    @Override
    public void destroy() throws Exception {
        container.destroy();
        BeanFactory.getBean(StatsService.class).shutdownExecutor();
    }

    @Override
    public boolean isAutoStartup() {
        return container.isAutoStartup();
    }

    @Override
    public void stop(Runnable callback) {
        container.stop(callback);
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }

    @Override
    public int getPhase() {
        return container.getPhase();
    }

    public boolean getStats() {
        return stats;
    }

    public void setStats(boolean stats) {
        this.stats = stats;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public long getSyncInitialDelay() {
        return syncInitialDelay;
    }

    public void setSyncInitialDelay(long syncInitialDelay) {
        this.syncInitialDelay = syncInitialDelay;
    }

    public long getSyncDelay() {
        return syncDelay;
    }

    public void setSyncDelay(long syncDelay) {
        this.syncDelay = syncDelay;
    }

    public List<String> getSyncCacheNames() {
        return syncCacheNames;
    }

    public void setSyncCacheNames(List<String> syncCacheNames) {
        this.syncCacheNames = syncCacheNames;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
