package com.qhdp.config;

import com.qhdp.factory.BloomFilterHandlerFactory;
import com.qhdp.register.BloomFilterHandlerRegistrar;
import com.qhdp.register.BloomFilterProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(BloomFilterProperties.class)
public class BloomFilterConfiguration {
    @Bean
    public BloomFilterHandlerFactory bloomFilterHandlerFactory(){
        return new BloomFilterHandlerFactory();
    }

    @Bean
    public BloomFilterHandlerRegistrar bloomFilterHandlerRegistrar(Environment environment){
        return new BloomFilterHandlerRegistrar(environment);
    }
}
