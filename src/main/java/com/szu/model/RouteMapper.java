package com.szu.model;

import java.lang.reflect.Method;

public class RouteMapper {
    public Object bean;
    public Method method;

    public RouteMapper(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }
}