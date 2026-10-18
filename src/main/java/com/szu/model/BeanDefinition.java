package com.szu.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class BeanDefinition {
    public final Class<?> clazz;
    public final List<Field> fields;
    public final boolean isRoute;
    public final List<Method> postMethods;
    public final List<Method> getMethods;

    public BeanDefinition(Class<?> clazz, List<Field> fields, boolean isRoute, List<Method> postMethods, List<Method> getMethods) {
        this.clazz = clazz;
        this.fields = fields;
        this.isRoute = isRoute;
        this.postMethods = postMethods;
        this.getMethods = getMethods;
    }
}
