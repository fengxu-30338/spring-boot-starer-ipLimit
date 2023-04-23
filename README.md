简介

iplimit是一个简单而又强大的工具，能够很方便的实现接口的访问限制，对于一些超过访问限制的ip进行封禁一段时间，项目使用redis存储ip数据，天然支持分布式接口访问控制(v1.1.0-RELEASE版本及以后)！



## 1. 导入依赖

```xml
<!-- 添加仓库 -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<!-- 添加依赖 -->
<dependency>
    <groupId>com.github.fengxu-30338</groupId>
    <artifactId>spring-boot-starer-ipLimit</artifactId>
    <version>1.1.0-RELEASE</version>
</dependency>
```



## 2. 开始使用



1. 您需要在启动类上使用@EnableIpLimit注解开启ip限制功能

```java
@EnableIpLimit
@SpringBootApplication
public class xxxApplication {

    public static void main(String[] args) {
        SpringApplication.run(xxxApplication.class, args);
    }

}
```



2. 编写配置文件 application.yml

```yaml
# 项目使用redis，需要设置下redis相关信息
spring:
    redis:
        database: 0
        host: 192.168.0.1
        port: 6379

# 配置ip限制参数
fx-iplimit:
  redis-prefix-key: "fengxu/demo/iplimit" # redis中共本工具使用的前缀
  ip-detect-interval: 5000 # ip检测的时间间隔,单位ms
  ip-access-max-count-in-detect-interval: 5  # ip检测时间间隔内可访问的最大次数，超过则封禁
  ip-ban-time: 60000 # 封禁时间，单位ms
```



3. 在需要ip访问控制的controller类或方法上标注@FxIpLimit注解即可

```java
@FxIpLimit
@Controller
public class TagController {

    @Autowired
    private TagMapper tagMapper;

    // 获取tag分组列表
    @GetMapping("/test1")
    public Result test1(){
        ...
    }

    @FxIpLimit(ipAccessMaxCountInDetectInterval = 10)
    @GetMapping("/test2/{num}")
    public Result test2(@PathVariable("num") Integer num){
        ...
    }
}
```

@FxIpLimit中参数优先级为:  方法 > 类 > 配置文件

其共有三个参数

1. ipDetectInterval 
2. ipAccessMaxCountInDetectInterval
3. ipBanTime

三个参数分别对应配置文件中的那三个，这里就不在赘述了



## 3. 扩展使用

1.可以通过实现IpLimitHandler接口来自定义实现ip封禁后的自定义处理

```java
/**
 * 处理ip封禁后的错误提示
 */
@Component
public class IpBanHandler implements IpLimitHandler {
    
    // ipAccessEntity可以获取到访问的 (ip,访问次数，最后访问时刻，访问方法全限定名) 信息
    @Override
    public String onIpBaned(IpAccessEntity ipAccessEntity) {
        if (ipAccessEntity.getAccessCount() < 50) {
            return JSONObject.toJSONString(new Result().forbidError());
        }
        return JSONObject.toJSONString(new Result(403, "就这点本事? 憨批蛋子!", null));
    }

    @Override
    public String unknowError() {
        return JSONObject.toJSONString(new Result().unknownError());
    }
}
```



2. 您可以通过继承FxIpLimitInterceptor在将其注入容器中来实现更高程度的自定义，这个就不在演示了，自行探索吧