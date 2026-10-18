package com.szu.core;

import com.szu.annotation.GetRequest;
import com.szu.annotation.PostRequest;
import com.szu.annotation.Sonnet;
import com.szu.model.BeanDefinition;
import com.szu.model.RouteMapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RouteRegistry {
    private final Map<Class<?>, BeanDefinition> beanDefinitions;
    private final Map<Class<?>, Object> beans;

    RouteRegistry(Map<Class<?>, Object> beans, Map<Class<?>, BeanDefinition> beanDefinitions) {
        this.beanDefinitions = beanDefinitions;
        this.beans = beans;
    }

    public Map<String, RouteMapper> register() {
        Map<String, RouteMapper> routes = new HashMap<>();

        for (Map.Entry<Class<?>, BeanDefinition> entry : beanDefinitions.entrySet()) {
            // 判断是否为路由
            BeanDefinition definition = entry.getValue();
            if (!definition.isRoute) continue;

            Class<?> clazz = definition.clazz;
            Object bean = beans.get(clazz);
            String first = clazz.getAnnotation(Sonnet.class).value();

            // 注册post方法
            for (Method method : definition.postMethods) {
                // 检查地址合法性
                String last = method.getAnnotation(PostRequest.class).value();
                if (first.isBlank() || last.isBlank()) {
                    throw new IllegalStateException("Route path is empty");
                }
                if (!first.startsWith("/") || !last.startsWith("/")) {
                    throw new IllegalStateException("Route path should be like /xxx");
                }
                String url = first + last;

                // 不允许重复注册路径
                if (routes.containsKey(url)) {
                    throw new IllegalStateException("Duplicate route: " + url);
                }

                method.setAccessible(true);
                routes.put(url, new RouteMapper(bean, method));
            }

            // 注册get方法
            for (Method method : definition.getMethods) {
                // 检查地址合法性
                String last = method.getAnnotation(GetRequest.class).value();
                if (first.isBlank() || last.isBlank()) {
                    throw new IllegalStateException("Route path is empty");
                }
                if (!first.startsWith("/") || !last.startsWith("/")) {
                    throw new IllegalStateException("Route path should be like /xxx");
                }
                String url = first + last;

                // 不允许重复注册路径
                if (routes.containsKey(url)) {
                    throw new IllegalStateException("Duplicate route: " + url);
                }

                method.setAccessible(true);
                routes.put(url, new RouteMapper(bean, method));
            }
        }

        return routes;
    }
}