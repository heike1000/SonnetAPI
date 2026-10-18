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

    public void createBeanDefinition() {
        for (Class<?> clazz : classes) {
            // 未标注则跳过
            if (!(clazz.isAnnotationPresent(Sonnet.class) || clazz.isAnnotationPresent(Verse.class))) continue;

            // 添加字段
            List<Field> fields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Refrain.class)) {
                    fields.add(field);
                }
            }

            // 是否为路由，如果是则添加路由方法
            boolean isRoute = clazz.isAnnotationPresent(Sonnet.class);
            List<Method> postMethods = new ArrayList<>();
            List<Method> getMethods = new ArrayList<>();
            if (isRoute) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(PostRequest.class)) {
                        // 检查参数合法性
                        int count = method.getParameterCount();
                        if (count > 1) {
                            throw new IllegalArgumentException();
                        } else if (count == 1) {
                            if (!(method.getParameters()[0].isAnnotationPresent(JsonBody.class) &&
                                    method.getParameterTypes()[0] == JsonObject.class)) {
                                throw new IllegalArgumentException();
                            }
                        }

                        // 检查返回值合法性
                        if (!JsonElement.class.isAssignableFrom(method.getReturnType())) {
                            throw new IllegalStateException("Route " + method + " must return JsonElement");
                        }

                        postMethods.add(method);
                    } else if (method.isAnnotationPresent(GetRequest.class)) {
                        // 检查参数合法性
                        int count = method.getParameterCount();
                        if (count > 1) {
                            throw new IllegalArgumentException();
                        } else if (count == 1) {
                            if (!(method.getParameters()[0].isAnnotationPresent(QueryParam.class) &&
                                    Map.class.isAssignableFrom(method.getParameterTypes()[0]))) {
                                throw new IllegalArgumentException();
                            }
                        }

                        // 检查返回值合法性
                        if (!JsonElement.class.isAssignableFrom(method.getReturnType())) {
                            throw new IllegalStateException("Route " + method + " must return JsonElement");
                        }

                        getMethods.add(method);
                    }
                }
            }

            BeanDefinition beanDefinition = new BeanDefinition(clazz, fields, isRoute, postMethods, getMethods);
            beanDefinitions.put(clazz, beanDefinition);
        }
    }

    public Map<Class<?>, Object> createBeans() throws Exception {
        for (Class<?> clazz : beanDefinitions.keySet()) {
            getBean(clazz);
        }

        return beans;
    }

    // 二级缓存注入
    public Object getBean(Class<?> clazz) throws Exception {
        if (beans.containsKey(clazz)) return beans.get(clazz);
        if (earlyBeans.containsKey(clazz)) return earlyBeans.get(clazz);

        if (beanDefinitions.get(clazz) == null) {
            throw new IllegalStateException("No bean definition found for " + clazz.getName());
        }

        Object bean = clazz.getDeclaredConstructor().newInstance();
        earlyBeans.put(clazz, bean);

        for (Field field : beanDefinitions.get(clazz).fields) {
            field.setAccessible(true);
            field.set(bean, getBean(field.getType()));
        }

        earlyBeans.remove(clazz);
        beans.put(clazz, bean);

        return bean;
    }

    public Map<Class<?>, BeanDefinition> getBeanDefinitions() {
        return beanDefinitions;
    }
}
