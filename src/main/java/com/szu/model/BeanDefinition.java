package com.szu.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class BeanDefinition {
    public final Class<?> clazz; // 类元信息
    public final List<Field> fields; // 字段集合
    public final boolean isRoute; // 是否为路由
    public final List<Method> POSTMethods; // POST方法合集
    public final List<Method> GETMethods; // GET方法合集

    public BeanDefinition(Class<?> clazz, List<Field> fields, boolean isRoute, List<Method> POSTMethods, List<Method> GETMethods) {
        this.clazz = clazz;
        this.fields = fields;
        this.isRoute = isRoute;
        this.POSTMethods = POSTMethods;
        this.GETMethods = GETMethods;
    }
}
