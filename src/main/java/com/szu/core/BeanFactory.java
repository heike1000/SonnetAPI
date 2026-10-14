package com.szu.core;

import com.szu.annotation.Sonnet;
import com.szu.annotation.Verse;
import com.szu.annotation.Refrain;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanFactory {
    private final List<Class<?>> classes;
    private final Map<Class<?>, Object> beans = new HashMap<>();

    BeanFactory(List<Class<?>> classes) {
        this.classes = classes;
    }

    public Map<Class<?>, Object> createBeans() {
        // 遍历所有带有Verse与Sonnet的类并做成bean，全量制作避免循环依赖
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Verse.class) || clazz.isAnnotationPresent(Sonnet.class)) {
                // 只支持无参数构造
                try {
                    Object bean = clazz.getDeclaredConstructor().newInstance();
                    beans.put(clazz, bean);
                } catch (Exception e) {
                    throw new RuntimeException("Bean " + clazz.getName() + " must have a no-arg constructor");
                }
            }
        }

        return beans;
    }

    public void injection() throws Exception {
        for (Object bean : beans.values()) {
            Class<?> clazz = bean.getClass();

            // 沿继承链向上遍历，注入父类中的字段
            while (clazz != Object.class) {
                Field[] fields = clazz.getDeclaredFields();

                for (Field field : fields) {
                    if (field.isAnnotationPresent(Refrain.class)) {
                        // 精确定位同名不同包的类
                        Class<?> fieldType = field.getType();
                        Object targetBean = beans.get(fieldType);
                        if (targetBean == null) {
                            throw new RuntimeException("Bean not founded");
                        }

                        // @Sonnet只做路由，不允许被注入
                        if (fieldType.isAnnotationPresent(Sonnet.class)) {
                            throw new RuntimeException("@Sonnet cannot be injected via @Refrain");
                        }

                        // 进行注入，显式处理static字段
                        if (Modifier.isStatic(field.getModifiers())) {
                            throw new RuntimeException("Static field is not supported");
                        } else {
                            field.setAccessible(true);
                            field.set(bean, targetBean);
                        }
                    }
                }

                clazz = clazz.getSuperclass();
            }
        }
    }
}
