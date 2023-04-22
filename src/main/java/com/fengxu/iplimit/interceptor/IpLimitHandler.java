package com.fengxu.iplimit.interceptor;

/**
 * ip限制后处理操作接口
 */
public interface IpLimitHandler {

    /**
     * 当ip限制后操作
     * @param ipAccess ip访问信息
     * @return 显示给用户的错误信息
     */
    String onIpBaned(IpAccessEntity ipAccess);

    /**
     * 获取ip地址等异常情况出现后返回额值
     *
     * @return 显示给用户的错误信息
     */
    String unknowError();
}
