package com.github.zomin.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.zomin.redis.serializer.RedisKeySerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * @author Kalend
 * Redis 配置类
 */
@Configuration
public class RedisAutoConfig extends CachingConfigurerSupport {
    @Value("${spring.redis.host}")
    private String hostName;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.password:}")
    private String password;
    @Value("${spring.redis.database}")
    private int database;
    @Value("${spring.redis.jedis.pool.max-active:8}")
    private int maxActive;
    @Value("${spring.redis.jedis.pool.max-idle:8}")
    private int maxIdle;
    @Value("${spring.redis.jedis.pool.min-idle:0}")
    private int minIdle;
    @Value("${spring.redis.jedis.pool.max-wait:1000}")
    private int maxWait;
    @Value("${spring.redis.timeout:10000}")
    private long timeout;
    @Value("${redis.generic.pool.time-between-eviction-runs:1000}")
    private long timeBetweenEvictionRunsMillis;
    @Value("${redis.generic.pool.min-evictable-idle-timemillis:0}")
    private long minEvictableIdleTimeMillis;
    @Value("${redis.generic.pool.num-tests-per-eviction-run:-1}")
    private int numTestsPerEvictionRun;
    @Value("${redis.generic.pool.max-waitmillis:1000}")
    private long maxWaitMillis;
    @Value("${redis.generic.pool.soft-min-evictable-idle-timemillis:50000}")
    private long softMinEvictableIdleTimeMillis;
    @Value("${redis.generic..test-on-borrow:false}")
    private boolean testOnBorrow;
    @Value("${redis.generic.pool.test-on-return:false}")
    private boolean testOnReturn;
    @Value("${redis.generic.pool.test-on-create:false}")
    private boolean testOnCreate;
    @Value("${redis.generic.pool.test-while-idle:true}")
    private boolean testWhileIdle;
    @Value("${redis.generic.pool.block-when-exhausted:true}")
    private boolean blockWhenExhausted;
    @Value("${redis.generic.pool.jmx-enabled:true}")
    private boolean jmxEnabled;

    @Bean
    public GenericObjectPoolConfig genericObjectPoolConfig() {
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        genericObjectPoolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        genericObjectPoolConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        genericObjectPoolConfig.setMaxWaitMillis(maxWaitMillis);
        genericObjectPoolConfig.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
        genericObjectPoolConfig.setTestOnBorrow(testOnBorrow);
        genericObjectPoolConfig.setTestOnReturn(testOnReturn);
        genericObjectPoolConfig.setTestOnCreate(testOnCreate);
        genericObjectPoolConfig.setTestWhileIdle(testWhileIdle);
        genericObjectPoolConfig.setBlockWhenExhausted(blockWhenExhausted);
        genericObjectPoolConfig.setJmxEnabled(jmxEnabled);
        return genericObjectPoolConfig;
    }

    @Bean
    public JedisClientConfiguration jedisClientConfiguration(GenericObjectPoolConfig genericObjectPoolConfig){
        JedisClientConfiguration.DefaultJedisClientConfigurationBuilder builder = (JedisClientConfiguration.DefaultJedisClientConfigurationBuilder) JedisClientConfiguration
            .builder()
            .connectTimeout(Duration.ofSeconds(timeout))
            .usePooling()
            .poolConfig(genericObjectPoolConfig);
        return builder.build();
    }

    @Bean
    public RedisStandaloneConfiguration config() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(hostName);
        config.setPort(port);
        config.setPassword(RedisPassword.of(password));
        config.setDatabase(database);
        return config;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory(RedisStandaloneConfiguration config,
                                                         JedisClientConfiguration jedisClientConfiguration) {
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(config, jedisClientConfiguration);
        // 连接池初始化
        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }

    /**
     * value 值 序列化
     *
     * @return RedisSerializer
     */
    @Bean
    @ConditionalOnMissingBean(RedisSerializer.class)
    @SuppressWarnings("findsecbugs:JACKSON_UNSAFE_DESERIALIZATION")
    public RedisSerializer<Object> redisSerializer() { //NOSONAR
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(
            Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.registerModule(new JavaTimeModule());
        //防止 BigDecimal转化时出现精度丢失
        om.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        // 去掉各种类似@JsonSerialize注解的解析
        om.configure(MapperFeature.USE_ANNOTATIONS, false);
        // 遇到未知属性是否抛出异常 ，默认是抛出异常的
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 不包含任何属性的bean也不报错
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 将类型序列化到属性json字符串中，此项必须配置，否则会报java.lang.ClassCastException: java.util.LinkedHashMap cannot be cast to XXX
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        // 属性为Null的不进行序列化，只对pojo起作用，对map和list不起作用
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }



    @Bean(name = "redisTemplate")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory jedisConnectionFactory,
                                                       RedisSerializer<Object> redisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(jedisConnectionFactory);
        RedisKeySerializer redisKeySerializer = new RedisKeySerializer();
        // key 序列化
        template.setKeySerializer(redisKeySerializer);
        template.setHashKeySerializer(redisKeySerializer);
        // value 序列化
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
