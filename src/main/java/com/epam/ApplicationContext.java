package com.epam;

import lombok.Getter;
import lombok.Setter;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toMap;

/**
 * @author Evgeny Borisov
 */
public class ApplicationContext {
    @Setter
    private ObjectFactory factory;
    private Map<Class, Object> cache = new ConcurrentHashMap<>();
    @Getter
    private Config config;

    public ApplicationContext(Config config) {
        this.config = config;
    }

    public <T> T getObject(Class<T> type) {
        if (cache.containsKey(type)) {
            return (T) cache.get(type);
        }

        Class<? extends T> implClass = type;

        if (type.isInterface()) {
            implClass = config.getImplClass(type);
        }
        T t = factory.createObject(implClass);

        if (implClass.isAnnotationPresent(Singleton.class)) {
            cache.put(type, t);
        }

        return t;
    }

    public void scanAndInitSingletons() {
        Reflections scanner = config.getScanner();
        scanner.getTypesAnnotatedWith(Singleton.class).stream()
                .filter(aClass -> !aClass.isAnnotationPresent(Lazy.class))
                .forEach(this::pushWithAllParentInterfaces);
    }

    private <T> void pushWithAllParentInterfaces(Class<T> type) {

        Class<? extends T> implClass = type;

        List<Class<?>> keys = new ArrayList<>();
        keys.add(type);

        if (type.isInterface()) {
            implClass = config.getImplClass(type);
        } else {
            keys.addAll(Arrays.asList(type.getInterfaces()));
        }


        T t = factory.createObject(implClass);

        keys.forEach(aClass -> cache.put(aClass, t));
    }
}
