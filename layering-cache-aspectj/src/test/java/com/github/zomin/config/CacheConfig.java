package com.github.zomin.config;

import com.github.zomin.aspect.LayeringAspect;
import com.github.zomin.manager.CacheManager;
import com.github.zomin.manager.LayeringCacheManager;
import com.github.zomin.test.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@Import({RedisConfig.class})
@EnableAspectJAutoProxy
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        LayeringCacheManager layeringCacheManager = new LayeringCacheManager(redisTemplate);
        // 开启统计功能
        layeringCacheManager.setStats(true);
        return layeringCacheManager;
    }

    @Bean
    public LayeringAspect layeringAspect() {
        return new LayeringAspect();
    }

    @Bean
    public TestService testService() {
        return new TestService();
    }
}
