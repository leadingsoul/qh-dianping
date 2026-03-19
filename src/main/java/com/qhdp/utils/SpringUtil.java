package com.qhdp.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static com.qhdp.constant.Constant.DEFAULT_PREFIX_DISTINCTION_NAME;
import static com.qhdp.constant.Constant.PREFIX_DISTINCTION_NAME;

/**
 * @description: spring工具
 * @author: phoenix
 **/
@Component
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static Environment environment;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
        environment = context.getEnvironment();
    }

    public static String getPrefixDistinctionName(){
        return environment.getProperty(PREFIX_DISTINCTION_NAME, DEFAULT_PREFIX_DISTINCTION_NAME);
    }
}