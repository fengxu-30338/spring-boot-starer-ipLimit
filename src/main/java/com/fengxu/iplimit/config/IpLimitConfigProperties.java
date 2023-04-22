package com.fengxu.iplimit.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ip limit配置类
 * 需要配置以下参数和spring.redis.*参数
 */
@Data
@ToString
@NoArgsConstructor
public class IpLimitConfigProperties {

    /**
     * IP访问检测时间间隔: ms
     */
    private int ipDetectInterval;

    /**
     * ip检测间隔时间内的最大访问次数
     */
    private int ipAccessMaxCountInDetectInterval;

    /**
     * ip的封禁时间: ms
     */
    private long ipBanTime;

    /**
     * redis存储ip封禁信息的前缀key
     */
    private String redisPrefixKey;
}
