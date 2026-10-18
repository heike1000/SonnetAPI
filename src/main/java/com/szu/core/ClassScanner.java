package com.szu.core;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassScanner {
    private final String basePackage;

    ClassScanner(String basePackage) {
        this.basePackage = basePackage;
    }

    public List<Class<?>> getClasses() throws Exception {
        String path = basePackage.replace('.', '/');
        // 获取target目录下对应路径
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

        List<Class<?>> classes = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            // 支持双扫
            if ("file".equals(protocol)) {
                scanDirectory(resource, basePackage, classes, seen);
            } else if ("jar".equals(protocol)) {
                scanJar(resource, path, classes, seen);
            }
        }

        return classes;
    }

    // 编译前
    private void scanDirectory(URL resource, String basePackage, List<Class<?>> classes, Set<String> seen) throws Exception {
        File root = new File(resource.toURI());
        Deque<File> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            File directory = queue.poll();
            File[] files = directory.listFiles();
            if (files == null) continue;

            for (File file : files) {
                if (file.isDirectory()) {
                    queue.add(file);
                    continue;
                }
                if (!file.getName().endsWith(".class")) continue;

                // 获取类相对于根目录的路径，删除class后缀
                String relative = root.toURI().relativize(file.toURI()).getPath();
                String classFileName = relative.substring(0, relative.length() - ".class".length());

                // 跨系统支持并拼接
                String classSuffix = classFileName.replace('/', '.').replace('\\', '.');
                String className = basePackage + "." + classSuffix;

                Class<?> clazz = Class.forName(className);
                if (seen.add(className)) {
                    classes.add(clazz);
                }
            }
        }
    }

    // 编译后
    private void scanJar(URL resource, String path, List<Class<?>> classes, Set<String> seen) throws Exception {
        String jarPath = resource.getPath();
        int sep = jarPath.indexOf("!/");

        String jarFileUrl = jarPath.substring(0, sep);
        String decoded = URLDecoder.decode(jarFileUrl, StandardCharsets.UTF_8);
        decoded = decoded.substring("file:".length());

        try (JarFile jarFile = new JarFile(decoded)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            String prefix = path.endsWith("/") ? path : path + "/";

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                if (!name.endsWith(".class")) continue;
                if (!name.startsWith(prefix)) continue;

                String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');

                Class<?> clazz = Class.forName(className);
                if (seen.add(className)) {
                    classes.add(clazz);
                }
            }
        }
    }
}