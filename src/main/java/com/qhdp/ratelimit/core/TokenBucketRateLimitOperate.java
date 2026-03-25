package com.qhdp.ratelimit.core;

import com.qhdp.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * @description: 令牌
 * @author: phoenix
 **/
@Slf4j
public class TokenBucketRateLimitOperate {

    private final RedisUtils redisUtils;

    public TokenBucketRateLimitOperate(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    private DefaultRedisScript<Long> redisScript;

    @PostConstruct
    public void init(){
        try {
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/tokenBucket.lua")));
            redisScript.setResultType(Long.class);
        } catch (Exception e) {
            log.error("TokenBucketRateLimitOperate init lua error", e);
        }
    }

    public Long execute(List<String> keys, String[] args){
        return (Long)redisUtils.execute(redisScript, keys, args);
    }
}