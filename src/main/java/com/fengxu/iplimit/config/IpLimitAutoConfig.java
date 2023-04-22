package com.fengxu.iplimit.config;

import com.fengxu.iplimit.EnableIpLimit;
import com.fengxu.iplimit.interceptor.DefaultIpLimitHandler;
import com.fengxu.iplimit.interceptor.FxIpLimitInterceptor;
import com.fengxu.iplimit.interceptor.IpLimitHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackageClasses = EnableIpLimit.class)
public class IpLimitAutoConfig implements WebMvcConfigurer {

    @Autowired
    private FxIpLimitInterceptor ipLimitInterceptor;

    @ConfigurationProperties(prefix = "fx-iplimit")
    @Bean
    public IpLimitConfigProperties ipLimitConfigProperties() {
        return new IpLimitConfigProperties();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisTemplate redisTemplate) {
        RedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setStringSerializer(stringSerializer);
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);
        return redisTemplate;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipLimitInterceptor)
                .addPathPatterns("/**");
    }
}
