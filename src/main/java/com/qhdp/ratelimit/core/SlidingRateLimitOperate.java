package com.qhdp.ratelimit.core;

import com.qhdp.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * @description: 滑动
 * @author: phoenix
 **/
@Slf4j
public class SlidingRateLimitOperate {

    private final RedisUtils redisUtils;

    public SlidingRateLimitOperate(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    private DefaultRedisScript<Long> redisScript;

    @PostConstruct
    public void init(){
        try {
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rateLimitSliding.lua")));
            redisScript.setResultType(Long.class);
        } catch (Exception e) {
            log.error("SlidingRateLimitOperate init lua error", e);
        }
    }

    public Long execute(List<String> keys, String[] args){
        return (Long)redisUtils.execute(redisScript, keys, args);
    }
}