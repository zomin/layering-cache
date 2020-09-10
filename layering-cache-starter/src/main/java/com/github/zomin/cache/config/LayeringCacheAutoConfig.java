package com.github.zomin.cache.config;

import com.github.zomin.aspect.LayeringAspect;
import com.github.zomin.cache.LayeringCacheUtils;
import com.github.zomin.cache.properties.LayeringCacheProperties;
import com.github.zomin.manager.CacheManager;
import com.github.zomin.manager.LayeringCacheManager;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 多级缓存自动配置类
 *
 * @author kalend.zhang
 */
@Configuration
@AutoConfigureAfter( {RedisAutoConfig.class})
@EnableAspectJAutoProxy
@EnableConfigurationProperties({LayeringCacheProperties.class})
@Import({LayeringCacheServletConfiguration.class})
public class LayeringCacheAutoConfig {
    @Bean(name="layerCacheManager")
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager layeringCacheManager(RedisTemplate<String, Object> redisTemplate, LayeringCacheProperties properties) {
        LayeringCacheManager layeringCacheManager = new LayeringCacheManager(redisTemplate);
        // 缓存统计功能
        layeringCacheManager.setStats(properties.getStats().isEnabled());
        layeringCacheManager.setInitialDelay(properties.getStats().getInitialDelay());
        layeringCacheManager.setDelay(properties.getStats().getDelay());

        // 缓存主动刷新功能
        layeringCacheManager.setSync(properties.getSync().isEnabled());
        layeringCacheManager.setSyncInitialDelay(properties.getSync().getInitialDelay());
        layeringCacheManager.setSyncDelay(properties.getSync().getDelay());
        layeringCacheManager.setSyncCacheNames(properties.getSync().getCacheKeys());
        return layeringCacheManager;
    }

    @Bean
    public LayeringAspect layeringAspect() {
        return new LayeringAspect();
    }

    @Bean
    public LayeringCacheUtils layeringCacheUtils() {return new LayeringCacheUtils();}

}
