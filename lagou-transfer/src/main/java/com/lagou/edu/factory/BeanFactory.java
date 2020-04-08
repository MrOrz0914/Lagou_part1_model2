package com.lagou.edu.factory;

import com.alibaba.druid.util.StringUtils;
import com.lagou.edu.annotation.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 应癫
 * <p>
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String, Object> map = new HashMap<>();  // 存储对象


    static {
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();

            List<Element> scans = rootElement.selectNodes("//component-scan");
            String scanPath = scans.get(0).attributeValue("base-package");

            scanPath = scanPath.replace(".", File.separator);
            String path = Thread.currentThread().getContextClassLoader().getResource("").getPath() + scanPath.replaceAll("\\.", "/");

            // 扫描配置路径的注解
            scanAnnotation(path);

            // 属性注入
            injectProperties();

            // 单独解析事务
            executeTransactional();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void scanAnnotation(String path) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        File file = new File(path);
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                scanAnnotation(f.getPath());
            } else {
                String absolutePath = f.getAbsolutePath();
                String[] split = absolutePath.split("\\.");
                String s = split[0];
                int index = s.indexOf("com");
                String packagePath = s.substring(index);

                String classPath = packagePath.replace(File.separator, ".");
                Class<?> aClass = Class.forName(classPath);

                parserAnnotation(classPath, aClass);

            }
        }
    }

    private static void parserAnnotation(String classPath, Class fileClazz) throws IllegalAccessException, InstantiationException {

        Annotation[] annotations = fileClazz.getAnnotations();

        for (Annotation annotation : annotations) {
            String name;

            if (annotation instanceof MyService) {
                name = ((MyService) annotation).value();
            } else if (annotation instanceof MyRepository) {
                name = ((MyRepository) annotation).value();
            } else if (annotation instanceof MyComponent) {
                name = ((MyComponent) annotation).value();
            } else {
                continue;
            }

            if (StringUtils.isEmpty(name)) {
                name = toGetLowerName(classPath);
            }

            map.put(name, fileClazz.newInstance());
        }
    }

    private static void injectProperties() throws IllegalAccessException {
        for (Object value : map.values()) {
            Class<?> aClass = value.getClass();
            Field[] declaredFields = aClass.getDeclaredFields();
            for (Field field : declaredFields) {

                Annotation[] annotations = field.getAnnotations();

                for (Annotation annotation : annotations) {
                    boolean b = annotation instanceof MyAutowired;
                    if (!b) {
                        continue;
                    }

                    MyAutowired autowired = (MyAutowired) annotation;
                    String bean = autowired.value();
                    if (StringUtils.isEmpty(bean)) {
                        bean = toGetLowerName(field.getType().getSimpleName());
                    }
                    Object o = map.get(bean);
                    field.setAccessible(true);
                    field.set(value, o);
                }
            }
        }
    }

    private static String toGetLowerName(String string) {
        int i = string.lastIndexOf(".");
        String substring = string.substring(i + 1);
        String first = substring.substring(0, 1);
        return first.toLowerCase() + substring.substring(1);
    }

    private static void executeTransactional() {
        ProxyFactory proxyFactory = (ProxyFactory) map.get("proxyFactory");

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String beanName = entry.getKey();
            Object o = entry.getValue();
            Class<?> aClass = entry.getValue().getClass();
            if (aClass.isAnnotationPresent(MyTransactional.class)) {
                Class<?>[] interfaces = aClass.getInterfaces();
                if (interfaces != null && interfaces.length > 0) {
                    // 使用jdk动态代理
                    map.put(beanName, proxyFactory.getJdkProxy(o));
                } else {
                    // 使用cglib动态代理
                    map.put(beanName, proxyFactory.getCglibProxy(o));
                }
            }
        }
    }


    public static Object getBean(String id) {
        return map.get(id);
    }

}
