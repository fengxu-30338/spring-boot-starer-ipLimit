package com.fengxu.iplimit.interceptor;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fengxu.iplimit.pojo.CommonResult;
import lombok.SneakyThrows;

/**
 * 默认的ip限制后的处理方法
 */
public class DefaultIpLimitHandler implements IpLimitHandler {

    @SneakyThrows
    @Override
    public String onIpBaned(IpAccessEntity ipAccess) {
        JsonMapper jsonMapper = new JsonMapper();
        return jsonMapper.writeValueAsString(CommonResult.ipLimitError());
    }

    @SneakyThrows
    @Override
    public String unknowError() {
        JsonMapper jsonMapper = new JsonMapper();
        return jsonMapper.writeValueAsString(CommonResult.unknowError());
    }
}
