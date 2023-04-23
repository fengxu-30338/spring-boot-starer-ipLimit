package com.fengxu.iplimit.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fengxu.iplimit.anno.FxIpLimit;
import com.fengxu.iplimit.config.IpLimitConfigProperties;
import com.fengxu.iplimit.util.CommonUtils;
import com.fengxu.iplimit.util.RedissonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class FxIpLimitInterceptor implements HandlerInterceptor {

    // 用户配置的ip限制策略
    @Resource
    private IpLimitConfigProperties ipLimitConfigProperties;

    // 不同方法对应的ip限制策略
    private final Map<String, IpLimitConfig> ipLimitConfMap = new ConcurrentHashMap<>();

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private IpLimitHandler ipLimitHandler;

    @Resource
    private RedissonUtil redissonUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        // 获取方法全限名
        String methodCanonicalName= getMethodCanonicalName(handlerMethod.getBeanType(), handlerMethod.getMethod());
        String ipAddr = CommonUtils.getIpAddr(request);
        if (!StringUtils.hasText(ipAddr)) {
            returnUnknowError(response);
            return false;
        }
        // 获取当前项的ip限制配置
        IpLimitConfigProperties currentIpLimitConf = parseCurrentIpLimitProp(handlerMethod.getBeanType(), handlerMethod.getMethod());
        if (currentIpLimitConf == null) {
            return true;
        }

        String ipLimitRedisMap = getIpLimitRedisMapName(currentIpLimitConf.getRedisPrefixKey());
        String ipLimitKey = getIpLimitRedisKey(methodCanonicalName, ipAddr);
        String ipBanRedisMap = getIpBanRedisMapName(currentIpLimitConf.getRedisPrefixKey());
        String ipBanKey = getIpBanRedisKey(ipAddr);
        // 加分布式锁,只给指定ip访问的方法加上分布式锁，防止锁粒度过大,所以采用ipLimitKey即可
        if (!redissonUtil.tryLock(ipLimitKey, 7)) {
            returnUnknowError(response);
        }
        try {
            // 判断是否已经存在于封禁map中
            Object banTimeStr = redisTemplate.opsForHash().get(ipBanRedisMap, ipBanKey);
            if (banTimeStr != null) {
                Object json = redisTemplate.opsForHash().get(ipLimitRedisMap, ipLimitKey);
                if (json == null) {
                    returnUnknowError(response);
                    return false;
                }
                IpAccessEntity ipAccessEntity = deseriesIpAccess(String.valueOf(json));
                if (Long.parseLong(banTimeStr.toString()) >= System.currentTimeMillis()) {
                    // 还处于封禁期
                    ipAccessEntity.setAccessCount(ipAccessEntity.getAccessCount() + 1);
                    ipAccessEntity.setTimestamp(System.currentTimeMillis());
                    redisTemplate.opsForHash().put(ipLimitRedisMap, ipLimitKey, seriesIpAccess(ipAccessEntity));
                    returnIpBanError(response, ipAccessEntity);
                    return false;
                } else {
                    // 已经解封
                    redisTemplate.opsForHash().delete(ipBanRedisMap, ipBanKey);
                    redisTemplate.opsForHash().delete(ipLimitRedisMap, ipLimitKey);
                }
            }

            // 不存在直接加入
            if (!redisTemplate.opsForHash().hasKey(ipLimitRedisMap, ipLimitKey)) {
                IpAccessEntity ipAccessEntity = new IpAccessEntity(ipAddr, methodCanonicalName, System.currentTimeMillis(), 1);
                redisTemplate.opsForHash().put(ipLimitRedisMap, ipLimitKey, seriesIpAccess(ipAccessEntity));
                return true;
            }
            // 存在判断时间间隔
            Object json = redisTemplate.opsForHash().get(ipLimitRedisMap, ipLimitKey);
            if (json == null) {
                returnUnknowError(response);
                return false;
            }
            IpAccessEntity ipAccessEntity = deseriesIpAccess(String.valueOf(json));
            if (System.currentTimeMillis() < ipAccessEntity.getTimestamp()) {
                returnUnknowError(response);
                return false;
            }

            if (System.currentTimeMillis() - ipAccessEntity.getTimestamp() > currentIpLimitConf.getIpDetectInterval()) {
                // 大于检测时间间隔，则重制访问次数为1次
                ipAccessEntity.setAccessCount(1);
                ipAccessEntity.setTimestamp(System.currentTimeMillis());
                redisTemplate.opsForHash().put(ipLimitRedisMap, ipLimitKey, seriesIpAccess(ipAccessEntity));
                return true;
            } else {
                // 小于访问间隔计算访问次数是否大于限制
                ipAccessEntity.setAccessCount(ipAccessEntity.getAccessCount() + 1);
                if (ipAccessEntity.getAccessCount() > currentIpLimitConf.getIpAccessMaxCountInDetectInterval()) {
                    // 大于限制次数，计算解封时间，加入到解封时间map
                    ipAccessEntity.setTimestamp(System.currentTimeMillis());
                    long unsealTime = System.currentTimeMillis() + currentIpLimitConf.getIpBanTime();
                    redisTemplate.opsForHash().put(ipBanRedisMap, ipBanKey, String.valueOf(unsealTime));
                    returnIpBanError(response, ipAccessEntity);
                    return false;
                }
                redisTemplate.opsForHash().put(ipLimitRedisMap, ipLimitKey, seriesIpAccess(ipAccessEntity));
            }
        } finally {
            redissonUtil.unlock(ipLimitKey);
        }
        return true;
    }

    private String seriesIpAccess(IpAccessEntity accessEntity) throws Exception {
        JsonMapper jsonMapper = new JsonMapper();
        return jsonMapper.writeValueAsString(accessEntity);
    }

    private IpAccessEntity deseriesIpAccess(String json) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, IpAccessEntity.class);
    }

    // map: key: methodCanonicalName + ip, value: ipAccessEntity
    protected String getIpLimitRedisMapName(String redisPrefixKey) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(redisPrefixKey);
        stringBuilder.append("/_fx_ip-limit_/map");
        return stringBuilder.toString();
    }

    private String getIpLimitRedisKey(String methodCanonicalName, String ip) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(methodCanonicalName);
        stringBuilder.append('/');
        stringBuilder.append(ip);
        return stringBuilder.toString();
    }

    // map: key:ip, value:解封时间戳
    protected String getIpBanRedisMapName(String redisPrefixKey) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(redisPrefixKey);
        stringBuilder.append("/_fx_ip-ban_/map");
        return stringBuilder.toString();
    }

    private String getIpBanRedisKey(String ip) {
        return ip;
    }

    /**
     * 获取方法的全限定名
     */
    protected final String getMethodCanonicalName(Class<?> beanType, Method method) {
        return beanType.getCanonicalName() + "." + method.getName();
    }

    /**
     * 获取当前方法的限制策略
     *
     * @param beanType 方法所处bean的类型
     * @param method 方法引用
     * @return 限制策略,null表示该接口无需限制
     */
    protected final IpLimitConfigProperties parseCurrentIpLimitProp(Class<?> beanType, Method method) {
        String methodName = getMethodCanonicalName(beanType, method);
        if (ipLimitConfMap.containsKey(methodName)) {
            IpLimitConfig ipLimitConfig = ipLimitConfMap.get(methodName);
            if (ipLimitConfig.isExist()) {
                return ipLimitConfig.getIpLimitConfig();
            } else {
                return null;
            }
        }

        FxIpLimit clazzIpLimit = beanType.getAnnotation(FxIpLimit.class);
        FxIpLimit methodIpLimit = method.getAnnotation(FxIpLimit.class);
        // 方法和类都没有标注限制注解则说明该接口无需被限制
        if (Objects.isNull(clazzIpLimit) && Objects.isNull(methodIpLimit)) {
            ipLimitConfMap.put(methodName, IpLimitConfig.generateEmptyConfig());
            return null;
        }
        // iplimit的优先级为: 类注解->方法注解
        FxIpLimit realIpLimit = clazzIpLimit == null ? methodIpLimit :
                (methodIpLimit == null ? clazzIpLimit : methodIpLimit);
        IpLimitConfig ipLimitConfig;
        if (isAllDefaultConf(realIpLimit)) {
            // 全是默认配置则服用配置文件中设置的配置
            ipLimitConfig = new IpLimitConfig(true, ipLimitConfigProperties);
        } else {
            // 否则获取新配置
            IpLimitConfigProperties limitConfigProperties = new IpLimitConfigProperties();
            limitConfigProperties.setRedisPrefixKey(ipLimitConfigProperties.getRedisPrefixKey());
            limitConfigProperties.setIpBanTime(realIpLimit.ipBanTime() < 0 ?
                    ipLimitConfigProperties.getIpBanTime(): realIpLimit.ipBanTime());
            limitConfigProperties.setIpAccessMaxCountInDetectInterval(realIpLimit.ipAccessMaxCountInDetectInterval() < 0 ?
                    ipLimitConfigProperties.getIpAccessMaxCountInDetectInterval() : realIpLimit.ipAccessMaxCountInDetectInterval());
            limitConfigProperties.setIpDetectInterval(realIpLimit.ipDetectInterval() < 0 ?
                    ipLimitConfigProperties.getIpDetectInterval() : realIpLimit.ipDetectInterval());
            ipLimitConfig = new IpLimitConfig(true, limitConfigProperties);
        }
        ipLimitConfMap.put(methodName, ipLimitConfig);
        return ipLimitConfig.getIpLimitConfig();
    }

    private boolean isAllDefaultConf(FxIpLimit ipLimit) {
        return ipLimit.ipAccessMaxCountInDetectInterval() < 0 &&
                ipLimit.ipBanTime() < 0 &&
                ipLimit.ipDetectInterval() < 0;
    }

    private void returnUnknowError(HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ipLimitHandler.unknowError());
        response.getWriter().flush();
        response.getWriter().close();
    }

    private void returnIpBanError(HttpServletResponse response, IpAccessEntity ipAccessEntity) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ipLimitHandler.onIpBaned(ipAccessEntity));
        response.getWriter().flush();
        response.getWriter().close();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    protected static final class IpLimitConfig {

        private boolean exist;

        private IpLimitConfigProperties ipLimitConfig;

        public static IpLimitConfig generateEmptyConfig() {
            return new IpLimitConfig(false, null);
        }
    }
}
