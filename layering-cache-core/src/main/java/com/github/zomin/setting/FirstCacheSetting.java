package com.github.zomin.setting;

import com.github.zomin.support.ExpireMode;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 一级缓存配置项
 *
 * @author kalend.zhang
 */
public class FirstCacheSetting implements Serializable {
    /**
     * 缓存初始Size
     */
    private int initialCapacity = 10;

    /**
     * 缓存最大Size
     */
    private int maximumSize = 5000;

    /**
     * 缓存有效时间
     */
    private long expireTime = 30;

    /**
     * 缓存时间单位
     */
    private TimeUnit timeUnit = TimeUnit.MINUTES;

    /**
     * 缓存失效模式{@link ExpireMode}
     */
    private ExpireMode expireMode = ExpireMode.WRITE;

    public FirstCacheSetting() {
    }

    /**
     * @param initialCapacity 缓存初始Size
     * @param maximumSize     缓存最大Size
     * @param expireTime      缓存有效时间
     * @param timeUnit        缓存时间单位 {@link TimeUnit}
     * @param expireMode      缓存失效模式{@link ExpireMode}
     */
    public FirstCacheSetting(int initialCapacity, int maximumSize, long expireTime, TimeUnit timeUnit,
                             ExpireMode expireMode) {
        this.initialCapacity = initialCapacity;
        this.maximumSize = maximumSize;
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
        this.expireMode = expireMode;
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public void setInitialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public ExpireMode getExpireMode() {
        return expireMode;
    }

    public void setExpireMode(ExpireMode expireMode) {
        this.expireMode = expireMode;
    }

    public boolean isAllowNullValues() {
        return false;
    }
}
