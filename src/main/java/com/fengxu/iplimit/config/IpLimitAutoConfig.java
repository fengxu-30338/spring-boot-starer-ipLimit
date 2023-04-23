package com.fengxu.iplimit.config;

import com.fengxu.iplimit.EnableIpLimit;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Configuration
@ComponentScan(basePackageClasses = EnableIpLimit.class)
@Import(FxIpLimitWebConfig.class)
public class IpLimitAutoConfig{

    @Bean
    @SuppressWarnings("all")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        String url = StringUtils.hasText(redisProperties.getUrl()) ? redisProperties.getUrl() :
                "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress(url);
        singleServerConfig.setDatabase(redisProperties.getDatabase());
        if (Objects.nonNull(redisProperties.getPassword())) {
            singleServerConfig.setPassword(redisProperties.getPassword());
        }
        if (Objects.nonNull(redisProperties.getTimeout())) {
            singleServerConfig.setTimeout((int) redisProperties.getTimeout().toMillis());
        }
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
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
}
