🎼 Sonnet 框架
面向单机的高性能 Java API 服务器框架。

Sonnet 是一个轻量级、几乎零依赖（除 Gson 与 Undertow 外）的 Java Web 框架，灵感取自"十四行诗（Sonnet）"的优雅与规整。  
它通过注解驱动完成"类扫描 → Bean 装配 → 路由注册 → HTTP 请求分发"的全流程，让你专注于业务逻辑本身。  

✨ 特性一览  
🔍 自动扫描：递归扫描指定包下的所有类，支持 file 与 jar 双协议  
🫘 轻量 IoC：基于字段的依赖注入，支持循环依赖（二级缓存方案）  
🛣️ 注解路由：@Sonnet + @POSTRequest / @GETRequest 声明式注册路由  
⚡ 高性能反射：路由调用优先使用 MethodHandle，退化时自动回退 Method.invoke  
🧪 启动校验：参数个数、参数注解、返回类型、路径格式、重复路由等在启动期全部校验  
🧵 Undertow 内核：基于 Undertow 的高并发 IO，自动从 IO 线程派发到工作线程  
🗃️ 内置缓存：Couplet 提供带过期时间与 LRU 淘汰的线程安全本地缓存  
📦 JSON 原生：基于 Gson，请求体与响应体统一使用 JsonObject / JsonElement  

🧩 核心概念  
Sonnet 用一组富有诗意的注解来表达最朴素的概念：  
🎼 @Sonnet：标记一个路由控制器（可被路由注册，但不能被注入）  
🎵 @Verse：标记一个普通组件（可被依赖注入）  
🔗 @Refrain：标记需要自动注入的字段  
📮 @POSTRequest("/path")：声明一个 POST 接口  
📬 @GETRequest("/path")：声明一个 GET 接口  
📦 @JsonBody：POST 方法参数，类型必须为 JsonObject  
🔎 @QueryParam：GET 方法参数，类型必须为 Map<String, String>  

🗃️ 内置缓存 Couplet
🔁 随机 LRU，超过 maxSize 自动淘汰  
🧹 可选的后台清理线程（守护线程，不阻塞 JVM 退出）  
