package com.fengxu.iplimit.interceptor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记录ip访问信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpAccessEntity {

    /**
     * 访问者IP
     */
    private String ip;

    /**
     * 访问方法全限名
     */
    private String method;

    /**
     * 检测时间窗内首次访问的时间戳，封禁后表示实时访问的时间戳
     */
    private Long timestamp;

    /**
     * 检测时间窗内积累访问次数
     */
    private Integer accessCount;
}
