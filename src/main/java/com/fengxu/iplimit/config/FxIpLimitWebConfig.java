package com.fengxu.iplimit.config;

import com.fengxu.iplimit.interceptor.DefaultIpLimitHandler;
import com.fengxu.iplimit.interceptor.FxIpLimitInterceptor;
import com.fengxu.iplimit.interceptor.IpLimitHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FxIpLimitWebConfig implements WebMvcConfigurer {

    @Autowired
    private FxIpLimitInterceptor ipLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipLimitInterceptor)
                .addPathPatterns("/**");
    }

    @ConfigurationProperties(prefix = "fx-iplimit")
    @Bean
    public IpLimitConfigProperties ipLimitConfigProperties() {
        return new IpLimitConfigProperties();
    }

    @ConditionalOnMissingBean(FxIpLimitInterceptor.class)
    @Bean
    public FxIpLimitInterceptor getIpLimitInterceptor() {
        return new FxIpLimitInterceptor();
    }

    @ConditionalOnMissingBean(IpLimitHandler.class)
    @Bean
    public IpLimitHandler ipLimitHandler() {
        return new DefaultIpLimitHandler();
    }

}
