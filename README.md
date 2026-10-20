🎼 Sonnet 框架
面向单机的高性能 Java API 服务器框架。

Sonnet 是一个轻量级、几乎零依赖的 Java API 框架，灵感取自"十四行诗（Sonnet）"的优雅与规整。  
它通过注解驱动完成"类扫描 → Bean 装配 → 路由注册 → HTTP 请求分发"的全流程，让你专注于业务逻辑本身。  

✨ 特性一览  
1.轻量 IoC：基于字段的依赖注入，支持循环依赖（二级缓存方案）  
2.注解路由：@Sonnet + @POSTRequest / @GETRequest 声明式注册路由  
3.反射优化：路由调用优先使用 MethodHandle，减少反射开销  
4.Undertow 内核：基于 Undertow 的高并发 IO，自动从 IO 线程派发到工作线程  
5.内置缓存 Couplet：Couplet 提供带过期时间与随机 LRU 淘汰的本地缓存以及可选的后台清理线程    
6.JSON 原生：基于 Gson，请求体与响应体统一使用 JsonObject / JsonElement  

🧩 核心注解  
Sonnet 用一组富有诗意的注解来表达最朴素的概念：  
1.@Sonnet：标记一个路由控制器（可被路由注册，但不能被注入）  
2.@Verse：标记一个普通组件（可被依赖注入）  
3.@Refrain：标记需要自动注入的字段  
4.@POSTRequest("/path")：声明一个 POST 接口  
5.@GETRequest("/path")：声明一个 GET 接口  
6.@JsonBody：POST 方法参数，类型必须为 JsonObject  
7.@QueryParam：GET 方法参数，类型必须为 Map<String, String>

🚀 快速开始  
1.定义普通组件：用 @Verse 标记一个可被注入的普通组件，内部可以持有 Couplet 缓存
<details>
<summary>定义普通组件</summary>

```java
@Verse
public class ExampleClass {
    Couplet couplet = new Couplet(1000, 100L); // 1000：最大容量、100L：后台清理线程唤醒间隔

    public ExampleClass() {
        couplet.put("Peter", 20, 100L); // 100L：过期时间
    }

    public int getAge(String name) {
        return (int) couplet.get(name);
    }
}
```

</details>

2.定义路由控制器：用 @Sonnet 标记一个控制器，方法上用 @GETRequest / @POSTRequest 声明路由。字段上加 @Refrain 即可自动注入 @Verse 组件
<details>
<summary>定义路由控制器</summary>

```java
@Sonnet("/api")
public class ExampleAPI {
    @Refrain
    ExampleClass exampleClass;

    @GETRequest("/hello")
    public JsonObject response(@QueryParam Map<String, String> Params) {
        String name = Params.get("name");
        JsonObject result = new JsonObject();
        result.addProperty("age", exampleClass.getAge(name));
        return result;
    }
}
```

</details>

3.启动服务
<details>
<summary>启动服务</summary>

```java
public class Main {
    public static void main(String[] args) throws Exception {
        // 启动SonnetAPI
        SonnetAPIContext sonnetAPIContext = new SonnetAPIContext("com.szu");

        // 启动服务器
        WebServer webServer = new WebServer(1116, sonnetAPIContext.getRoutes());
        webServer.start();
    }
}
```

</details>