package com.github.zomin.tool.support;

/**
 * Servlet 初始化数据
 *
 * @author yuhao.wang3
 */
public class InitServletData {

    public static final String PARAM_NAME_ENABLE_UPDATE = "enableUpdate";

    public static final String RESOURCE_PATH = "http/resources";


    /**
     * 是否有更新数据权限
     */
    private Boolean enableUpdate;


    /**
     * 采集缓存命中数据的时间间隔，至少5分钟（单位分钟）
     */
    private long syncCacheStatsDelay = 5;


    public String getResourcePath() {
        return RESOURCE_PATH;
    }

    public long getSyncCacheStatsDelay() {
        return syncCacheStatsDelay;
    }

    public void setSyncCacheStatsDelay(long syncCacheStatsDelay) {
        this.syncCacheStatsDelay = syncCacheStatsDelay;
    }

    public Boolean getEnableUpdate() {
        return enableUpdate;
    }

    public void setEnableUpdate(Boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }
}
