package com.fengxu.iplimit.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求结果包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResult {

    /**
     * 返回数据
     */
    private Object data;

    /**
     * 饭hi消息
     */
    private String msg;

    /**
     * 返回错误码
     */
    private Integer code;

    /**
     * 未知错误
     * @return result
     */
    public static CommonResult unknowError() {
        return new CommonResult(null, "未知错误", 500);
    }

    /**
     * ip 封禁，禁止访问
     * @return result
     */
    public static CommonResult ipLimitError() {
        return new CommonResult(null, "Forbid Access", 403);
    }

    /**
     * 成功访问
     * @param data 返回的数据
     * @return result
     */
    public static CommonResult success(Object data) {
        return new CommonResult(data, "请求成功", 200);
    }
}
