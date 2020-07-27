package com.github.zomin.cache.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author yuhao.wang3
 */
@ConfigurationProperties("layering.cache")
public class LayeringCacheProperties {

    /**
     * 缓存统计
     */
    private Stats stats = new Stats();

    /**
     * 缓存主动刷新
     */
    private Sync sync = new Sync();


    /**
     * 命名空间，必须唯一般使用服务名
     */
    private String namespace;

    /**
     * 启动 LayeringCacheServlet.
     */
    private boolean layeringCacheServletEnabled = true;

    /**
     * contextPath
     */
    private String urlPattern;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public boolean isLayeringCacheServletEnabled() {
        return layeringCacheServletEnabled;
    }

    public void setLayeringCacheServletEnabled(boolean layeringCacheServletEnabled) {
        this.layeringCacheServletEnabled = layeringCacheServletEnabled;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public static class Stats {
        /**
         * 是否开启缓存统计
         */
        private boolean enabled = true;

        /**
         * 统计数据定时任务时间间隔，默认分钟，初始延迟 1
         */
        private long initialDelay = 1;

        /**
         * 统计数据定时任务时间间隔，默认分钟，频率 1
         */
        private long delay = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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
    }

    public static class Sync {
        /**
         * 是否开启缓存统计
         */
        private boolean enabled = true;

        /**
         * 统计数据定时任务时间间隔，默认分钟，初始延迟 1
         */
        private long initialDelay = 1;

        /**
         * 统计数据定时任务时间间隔，默认分钟，频率 1
         */
        private long delay = 1;

        /**
         * 需要主动刷新的Key
         */
        private List<String> cacheKeys;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

        public List<String> getCacheKeys() {
            return cacheKeys;
        }

        public void setCacheKeys(List<String> cacheKeys) {
            this.cacheKeys = cacheKeys;
        }
    }
}
