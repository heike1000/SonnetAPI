package com.szu.model;

import java.lang.reflect.Method;

public class RouteMapper {
    public final Object bean;
    public final Method method;

    public RouteMapper(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }
}