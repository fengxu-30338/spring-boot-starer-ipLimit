package com.fengxu.iplimit.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FxIpLimit {

    /**
     * IP访问检测时间间隔: ms(默认跟随配置)
     */
    int ipDetectInterval() default -1;

    /**
     * ip检测间隔时间内的最大访问次数(默认跟随配置)
     */
    int ipAccessMaxCountInDetectInterval() default -1;

    /**
     * ip的封禁时间: ms(默认跟随配置)
     */
    long ipBanTime() default -1L;
}
