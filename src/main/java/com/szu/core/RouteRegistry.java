package com.szu.core;

import com.szu.annotation.GETRequest;
import com.szu.annotation.POSTRequest;
import com.szu.annotation.Sonnet;
import com.szu.model.BeanDefinition;
import com.szu.model.RouteMapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RouteRegistry {
    private final Map<Class<?>, BeanDefinition> beanDefinitions;
    private final Map<Class<?>, Object> beans;
    private final Map<String, RouteMapper> routes = new HashMap<>();

    RouteRegistry(Map<Class<?>, Object> beans, Map<Class<?>, BeanDefinition> beanDefinitions) {
        this.beanDefinitions = beanDefinitions;
        this.beans = beans;
    }

    public Map<String, RouteMapper> register() {
        // 遍历definition，将每一个POST或GET的方法写入路由表
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            // 判断是否为路由
            if (!beanDefinition.isRoute) continue;
            Class<?> clazz = beanDefinition.clazz;
            Object bean = beans.get(clazz);

            // 注册POST方法
            for (Method method : beanDefinition.postMethods) {
                registerMethod(bean, method, "POST");
            }

            // 注册GET方法
            for (Method method : beanDefinition.getMethods) {
                registerMethod(bean, method, "GET");
            }
        }

        return routes;
    }

    private void registerMethod(Object bean, Method method, String methodType) {
        // 检查地址合法性
        String first = bean.getClass().getAnnotation(Sonnet.class).value();
        String last = "";
        if (methodType.equals("POST")) {
            last = method.getAnnotation(POSTRequest.class).value();
        } else if (methodType.equals("GET")) {
            last = method.getAnnotation(GETRequest.class).value();
        }
        
        if (first.isBlank() || last.isBlank()) {
            throw new IllegalStateException("Route path can't be empty in " + bean.getClass().getName() + "::" + method.getName());
        }
        if (!first.startsWith("/") || !last.startsWith("/") || first.endsWith("/") || last.endsWith("/")) {
            throw new IllegalStateException("Route path should be like /xxx in " + bean.getClass().getName() + "::" + method.getName());
        }
        String url = first + last;

        // 不允许重复注册路径
        if (routes.containsKey(url)) {
            throw new IllegalStateException("Duplicate route " + url);
        }

        method.setAccessible(true);
        routes.put(url, new RouteMapper(bean, method));
    }
}