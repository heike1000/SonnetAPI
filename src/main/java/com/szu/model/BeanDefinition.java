package com.szu.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class BeanDefinition {
    public final Class<?> clazz;
    public final List<Field> fields;
    public final boolean isRoute;
    public final List<Method> routeMethods;

    public BeanDefinition(Class<?> clazz, List<Field> fields, boolean isRoute, List<Method> routeMethods) {
        this.clazz = clazz;
        this.fields = fields;
        this.isRoute = isRoute;
        this.routeMethods = routeMethods;
    }
}
