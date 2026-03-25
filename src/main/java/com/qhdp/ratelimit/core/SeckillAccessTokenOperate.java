package com.qhdp.ratelimit.core;

import com.qhdp.utils.RedisUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.List;

/**
 * &#064;description: 校验
 * &#064;author: phoenix
 **/
public class SeckillAccessTokenOperate {

    private final DefaultRedisScript<Long> script;
    private final RedisUtils redisUtils;

    public SeckillAccessTokenOperate(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_access_token.lua")));
        this.script.setResultType(Long.class);
    }
    
    public boolean validateAndConsume(String key, String expected) {
        List<String> keys = Collections.singletonList(key);
        Long ret = (Long)redisUtils.execute(script, keys, expected);
        return ret == 1L;
    }
}