package com.knowledge.base.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring context utility class
 *
 * <p>Used to obtain beans from the Spring container</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * Get the ApplicationContext
     *
     * @return the ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Get a bean by type
     *
     * @param clazz the bean's type
     * @param <T>   generic type
     * @return the bean instance
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * Get a bean by name
     *
     * @param name the bean's name
     * @return the bean instance
     */
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    /**
     * Get a bean by name and type
     *
     * @param name  the bean's name
     * @param clazz the bean's type
     * @param <T>   generic type
     * @return the bean instance
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }
}
