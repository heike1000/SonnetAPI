package com.szu.model;

import com.google.gson.JsonElement;

import java.lang.invoke.*;
import java.lang.reflect.Method;

public class RouteMapper {
    @FunctionalInterface
    public interface HandlerInvoker {
        JsonElement invoke(Object arg) throws Throwable;
    }

    public final Object bean;
    public final Method method;
    public final HandlerInvoker invoker;

    public RouteMapper(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        // 使用methodhandler加快反射调用
        this.invoker = createInvoker(bean, method);
    }

    private static HandlerInvoker createInvoker(Object bean, Method method) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(bean.getClass(), MethodHandles.lookup());
            MethodHandle mh = lookup.unreflect(method);
            MethodHandle bound = mh.bindTo(bean);
            return arg -> (JsonElement) bound.invoke(arg);
        } catch (Throwable e) {
            return arg -> {
                try {
                    return (JsonElement) method.invoke(bean, arg);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }
    }
}