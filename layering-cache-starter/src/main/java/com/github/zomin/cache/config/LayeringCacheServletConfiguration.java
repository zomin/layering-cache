package com.github.zomin.cache.config;

import com.github.zomin.cache.properties.LayeringCacheProperties;
import com.github.zomin.tool.servlet.LayeringCacheServlet;
import com.github.zomin.util.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import javax.servlet.Servlet;

/**
 * Created LayeringCacheServletConfiguration by kalend.zhang on 2020/7/27.
 *
 * @author kalend.zhang
 */
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "layering.cache.layering-cache-servlet-enabled", havingValue = "true", matchIfMissing =
    false)
public class LayeringCacheServletConfiguration {
    @Bean
    public ServletRegistrationBean<Servlet> layeringCacheStatViewServletRegistrationBean(LayeringCacheProperties properties) {
        ServletRegistrationBean<Servlet> registrationBean = new ServletRegistrationBean<>();
        registrationBean.setServlet(new LayeringCacheServlet());
        registrationBean.addUrlMappings(!StringUtils.isEmpty(properties.getUrlPattern()) ? properties.getUrlPattern() : "/layering-cache/*");
        return registrationBean;
    }
}
