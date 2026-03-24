package com.qhdp.config;


import com.qhdp.constant.DelayQueueProperties;
import com.qhdp.delay.context.DelayQueueBasePart;
import com.qhdp.delay.context.DelayQueueContext;
import com.qhdp.handler.DelayQueueInitHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 延迟队列 配置
 * @author: phoenix
 **/
@Configuration
@EnableConfigurationProperties(DelayQueueProperties.class)
public class DelayQueueAutoConfig {
    
    @Bean
    public DelayQueueInitHandler delayQueueInitHandler(DelayQueueBasePart delayQueueBasePart){
        return new DelayQueueInitHandler(delayQueueBasePart);
    }
   
    @Bean
    public DelayQueueBasePart delayQueueBasePart(RedissonClient redissonClient,DelayQueueProperties delayQueueProperties){
        return new DelayQueueBasePart(redissonClient,delayQueueProperties);
    }
  
    @Bean
    public DelayQueueContext delayQueueContext(DelayQueueBasePart delayQueueBasePart){
        return new DelayQueueContext(delayQueueBasePart);
    }
}
