一个轻量级、高性能的单机 Java API 框架

Sonnet API 是一个面向单机部署场景的轻量级 Java Web 框架。它基于注解驱动、反射调用与 Undertow 高性能 HTTP 服务器构建，帮助开发者用极少的代码快速搭建 RESTful 风格的 JSON API 服务。

✨ 特性  
🚀 高性能：底层基于 Undertow，单机高并发场景下性能优异  
🎯 注解驱动：通过 @Sonnet、@Verse、@Refrain 等注解声明路由与依赖  
🧩 轻量 IOC 容器：内置两级缓存（beans + earlyBeans）实现字段注入，自动解决循环依赖  
🔍 自动类扫描：支持从目录（编译期）与 JAR 包（运行期）中扫描目标包下的所有类  
📦 零配置启动：一个 SonnetAPIContext 即可完成类扫描、Bean 装配与路由注册全流程