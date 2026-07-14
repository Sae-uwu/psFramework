package framework.util;

import framework.annotations.Controller;
import framework.annotations.UrlMapping;
import framework.models.UrlMethod;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasseUtilitaire {

    public static List<Class<?>> getClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            String packagePath = packageName.replace('.', '/');
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                if ("file".equals(protocol)) {
                    scanDirectory(new File(resource.toURI()), packageName, classes);
                } else if ("jar".equals(protocol)) {
                    scanJar(resource, packagePath, packageName, classes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

    public static List<Class<?>> getAnnotatedClasses(List<Class<?>> classes, Class<? extends Annotation> annotation) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotation)) {
                result.add(clazz);
            }
        }
        return result;
    }

    public static Map<String, Map<UrlMethod, Method>> getUrlMappingMap(String basePackage) {
        Map<String, Map<UrlMethod, Method>> controllerMap = new HashMap<>();
        List<Class<?>> allClasses = getClassesInPackage(basePackage);
        List<Class<?>> controllerClasses = getAnnotatedClasses(allClasses, Controller.class);

        for (Class<?> clazz : controllerClasses) {
            Controller controllerAnn = clazz.getAnnotation(Controller.class);
            String controllerName = controllerAnn.value();
            Map<UrlMethod, Method> methodMap = new HashMap<>();

            for (Method method : clazz.getDeclaredMethods()) {
                UrlMapping urlAnn = method.getAnnotation(UrlMapping.class);
                if (urlAnn != null) {
                    UrlMethod key = new UrlMethod(urlAnn.value(), urlAnn.method());
                    methodMap.put(key, method);
                }
            }

            if (!methodMap.isEmpty()) {
                controllerMap.put(controllerName, methodMap);
            }
        }

        return controllerMap;
    }

    private static void scanDirectory(File dir, String packageName, List<Class<?>> classes) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                }
            }
        }
    }

    private static void scanJar(URL resource, String packagePath, String packageName, List<Class<?>> classes) {
        String path = resource.getPath();
        String jarPath = path.substring(5, path.indexOf("!"));
        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(packagePath) && name.endsWith(".class") && !name.contains("$")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    try {
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
