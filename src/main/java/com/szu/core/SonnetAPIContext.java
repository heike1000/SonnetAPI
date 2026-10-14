package com.szu.core;

import com.szu.model.RouteMapper;

import java.util.*;

public class SonnetAPIContext {
    // IOC容器
    private final Map<Class<?>, Object> beans;

    // 路由表
    private final Map<String, RouteMapper> routes;

    public SonnetAPIContext(String basePackage) throws Exception {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("Base package must not be blank");
        }

        // 获取所有类
        ClassScanner classScanner = new ClassScanner(basePackage);
        List<Class<?>> classes = classScanner.getClasses();

        // 制作bean，进行自动装配
        BeanFactory beanFactory = new BeanFactory(classes);
        beans = beanFactory.createBeans();
        beanFactory.injection();

        // 生成路由表
        RouteRegistry routeRegistry = new RouteRegistry(beans);
        routes = routeRegistry.register();
    }

    public Map<String, RouteMapper> getRoutes() {
        return routes;
    }
}