package com.github.zomin.tool.servlet;


import com.alibaba.fastjson.JSON;
import com.github.zomin.manager.AbstractCacheManager;
import com.github.zomin.stats.CacheStatsInfo;
import com.github.zomin.tool.service.CacheService;
import com.github.zomin.tool.support.InitServletData;
import com.github.zomin.tool.support.Result;
import com.github.zomin.tool.support.URLConstant;
import com.github.zomin.util.BeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 统计的Servlet
 *
 * @author yuhao.wang3
 */
public class LayeringCacheServlet extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(LayeringCacheServlet.class);

    private InitServletData initServletData = new InitServletData();

    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void init() throws ServletException {

    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String requestURI = request.getRequestURI();

        response.setCharacterEncoding("utf-8");

        // root context
        if (contextPath == null) {
            contextPath = "";
        }

        String path = requestURI.substring(contextPath.length() + servletPath.length());

        // 重置缓存统计数据
        if (URLConstant.RESET_CACHE_STAT.equals(path)) {
            Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
            for (AbstractCacheManager cacheManager : cacheManagers) {
                cacheManager.resetCacheStat();
            }
            response.getWriter().write(JSON.toJSONString(Result.success()));
            return;
        }

        // 缓存统计列表
        if (URLConstant.CACHE_STATS_LIST.equals(path)) {
            String cacheName = request.getParameter("cacheName");
            Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
            List<CacheStatsInfo> statsList = new ArrayList<>();
            for (AbstractCacheManager cacheManager : cacheManagers) {
                List<CacheStatsInfo> cacheStats = cacheManager.listCacheStats(cacheName);
                if (!CollectionUtils.isEmpty(cacheStats)) {
                    statsList.addAll(cacheStats);
                }
            }
            response.getWriter().write(JSON.toJSONString(Result.success(statsList)));
            return;
        }

        // 删除缓存
        if (URLConstant.CACHE_STATS_DELETE_CACHW.equals(path)) {
            String cacheNameParam = request.getParameter("cacheName");
            String internalKey = request.getParameter("internalKey");
            String key = request.getParameter("key");
            BeanFactory.getBean(CacheService.class).deleteCache(cacheNameParam, internalKey, key);
            response.getWriter().write(JSON.toJSONString(Result.success()));
            return;
        }

        //查询所有缓存
        if(URLConstant.CACHE_FIND_ALL_CACHW.equals(path)) {
            String cacheNameParam = request.getParameter("cacheName");
            HashMap<String,Object> cacheMap = BeanFactory.getBean(CacheService.class).findAllCache(cacheNameParam);
            response.getWriter().write(JSON.toJSONString(Result.success(cacheMap)));
            return;
        }
    }

    private RedisTemplate<String, Object> getRedisTemplate() {
        Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
        for (AbstractCacheManager cacheManager : cacheManagers) {
            return cacheManager.getRedisTemplate();
        }
        return null;
    }
}
