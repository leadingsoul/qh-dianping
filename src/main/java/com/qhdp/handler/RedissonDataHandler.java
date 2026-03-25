package com.qhdp.handler;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * &#064;description:  redisson操作
 * &#064;author: phoenix
 **/
@Component
@RequiredArgsConstructor
public class RedissonDataHandler {
    
    private final RedissonClient redissonClient;
    
    public String get(String key){
        return (String)redissonClient.getBucket(key).get();
    }
    
    public void set(String key,String value){
        redissonClient.getBucket(key).set(value);
    }
    
    public void set(String key,String value,long timeToLive, TimeUnit timeUnit){
        redissonClient.getBucket(key).set(value,getDuration(timeToLive,timeUnit));
    }
    
    public Duration getDuration(long timeToLive, TimeUnit timeUnit){
        switch (timeUnit) {
            
            case MINUTES -> {
                return Duration.ofMinutes(timeToLive);
            }
            
            case HOURS -> {
                return Duration.ofHours(timeToLive);
            }
            
            case DAYS -> {
                return Duration.ofDays(timeToLive);
            }
            
            default -> {
                return Duration.ofSeconds(timeToLive);
            }
        }
    }
}
