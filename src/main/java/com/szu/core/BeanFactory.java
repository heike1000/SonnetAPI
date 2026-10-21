package com.szu.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.szu.annotation.*;
import com.szu.model.BeanDefinition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanFactory {
    private final List<Class<?>> classes;
    private final Map<Class<?>, BeanDefinition> beanDefinitions = new HashMap<>();
    private final Map<Class<?>, Object> beans = new HashMap<>();
    private final Map<Class<?>, Object> earlyBeans = new HashMap<>();

    BeanFactory(List<Class<?>> classes) {
        this.classes = classes;
    }

    public Map<Class<?>, BeanDefinition> createBeanDefinition() {
        for (Class<?> clazz : classes) {
            // 无注解则跳过
            if (!(clazz.isAnnotationPresent(Sonnet.class) || clazz.isAnnotationPresent(Verse.class))) continue;

            // 添加字段信息
            List<Field> fields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Refrain.class)) {
                    fields.add(field);
                }
            }

            // 添加路由信息
            boolean isRoute = clazz.isAnnotationPresent(Sonnet.class);
            List<Method> postMethods = new ArrayList<>();
            List<Method> getMethods = new ArrayList<>();
            if (isRoute) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(POSTRequest.class)) {
                        // 检查参数合法性
                        int count = method.getParameterCount();
                        if (count != 1) {
                            throw new IllegalArgumentException("Method " + clazz.getName() + "#" + method.getName() + " should have 1 parameter");
                        } else if (!(method.getParameters()[0].isAnnotationPresent(JsonBody.class) && method.getParameterTypes()[0] == JsonObject.class)) {
                            throw new IllegalArgumentException("Method " + clazz.getName() + "#" + method.getName() + "'s parameter should be like @JsonBody JsonObject xxx");
                        }

                        // 检查返回值合法性
                        if (!JsonElement.class.isAssignableFrom(method.getReturnType())) {
                            throw new IllegalStateException("Route " + clazz.getName() + "#" + method.getName() + " must return JsonElement");
                        }

                        postMethods.add(method);
                    } else if (method.isAnnotationPresent(GETRequest.class)) {
                        // 检查参数合法性
                        int count = method.getParameterCount();
                        if (count != 1) {
                            throw new IllegalArgumentException("Method " + clazz.getName() + "#" + method.getName() + " should have 1 parameter");
                        } else if (!(method.getParameters()[0].isAnnotationPresent(QueryParam.class) && Map.class.isAssignableFrom(method.getParameterTypes()[0]))) {
                            throw new IllegalArgumentException("Method " + clazz.getName() + "#" + method.getName() + "'s parameter should be like @QueryParam Map<String, String> xxx");
                        }

                        // 检查返回值合法性
                        if (!JsonElement.class.isAssignableFrom(method.getReturnType())) {
                            throw new IllegalStateException("Route " + clazz.getName() + "#" + method.getName() + " must return JsonElement");
                        }

                        getMethods.add(method);
                    }
                }
            }

            BeanDefinition beanDefinition = new BeanDefinition(clazz, fields, isRoute, postMethods, getMethods);
            beanDefinitions.put(clazz, beanDefinition);
        }

        return beanDefinitions;
    }

    public Map<Class<?>, Object> createBeans() throws Exception {
        for (Class<?> clazz : beanDefinitions.keySet()) {
            getBean(clazz);
        }

        return beans;
    }

    // 二级缓存注入
    public Object getBean(Class<?> clazz) throws Exception {
        // 命中则直接返回
        if (beans.containsKey(clazz)) return beans.get(clazz);
        if (earlyBeans.containsKey(clazz)) return earlyBeans.get(clazz);

        // 目标漏加注解，报错
        if (beanDefinitions.get(clazz) == null) {
            throw new IllegalStateException("Check your annotations. No bean definition found for " + clazz.getName());
        }

        // 创建bean，加入二级缓存
        Object bean = clazz.getDeclaredConstructor().newInstance();
        earlyBeans.put(clazz, bean);

        // 执行注入
        for (Field field : beanDefinitions.get(clazz).fields) {
            field.setAccessible(true);
            // Sonnet类型不能当成组件被注入
            if (field.getType().isAnnotationPresent(Sonnet.class)) {
                throw new IllegalStateException("@Sonnet type " + field.getType().getName() + " can not be inject via @Verse in " + clazz.getName());
            }
            //递归调用
            field.set(bean, getBean(field.getType()));
        }

        // 注入完成，加入一级缓存
        earlyBeans.remove(clazz);
        beans.put(clazz, bean);

        return bean;
    }
}
