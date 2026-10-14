package com.szu.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.szu.annotation.JsonBody;
import com.szu.annotation.PostRequest;
import com.szu.annotation.Sonnet;
import com.szu.model.RouteMapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RouteRegistry {
    private final Map<Class<?>, Object> beans;

    RouteRegistry(Map<Class<?>, Object> beans) {
        this.beans = beans;
    }

    public Map<String, RouteMapper> register() {
        Map<String, RouteMapper> routes = new HashMap<>();
        for (Object bean : beans.values()) {
            // 不是Sonnet则跳过
            if (!bean.getClass().isAnnotationPresent(Sonnet.class)) continue;

            for (Method method : bean.getClass().getDeclaredMethods()) {
                // 未设置路由则跳过
                if (!method.isAnnotationPresent(PostRequest.class)) continue;

                // 获取地址
                String first = bean.getClass().getAnnotation(Sonnet.class).value();
                String last = method.getAnnotation(PostRequest.class).value();

                // 检查地址合法性
                if (first.isBlank() || last.isBlank()) {
                    throw new IllegalStateException("Route path is empty");
                }
                if (!first.startsWith("/") || !last.startsWith("/")) {
                    throw new IllegalStateException("Route path should be like /xxx");
                }
                String url = first + last;

                // 检查参数合法性
                int count = method.getParameterCount();
                if (count > 1) {
                    throw new IllegalArgumentException();
                } else if (count == 1) {
                    if (method.getParameterTypes()[0] != JsonObject.class || !method.getParameters()[0].isAnnotationPresent(JsonBody.class)) {
                        throw new IllegalArgumentException();
                    }
                }

                // 检查返回值合法性
                if (!JsonElement.class.isAssignableFrom(method.getReturnType())) {
                    throw new IllegalStateException("Route method must return JsonElement: " + method);
                }

                // 不允许重复注册路径
                if (routes.containsKey(url)) {
                    throw new IllegalStateException("Duplicate route: " + url);
                } else {
                    method.setAccessible(true);
                    routes.put(url, new RouteMapper(bean, method));
                }
            }
        }

        return routes;
    }
}