package com.qhdp.kafka.lua;

import com.qhdp.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 回滚
 * @author: phoenix
 **/
@Slf4j
@Component
public class SeckillVoucherRollBackOperate {
    
    @Resource
    private RedisUtils redisUtils;
    
    private DefaultRedisScript<Long> redisScript;
    
    @PostConstruct
    public void init(){
        try {
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckillVoucherRollBack.lua")));
            redisScript.setResultType(Long.class);
        } catch (Exception e) {
            log.error("redisScript init lua error",e);
        }
    }
    
    public Integer execute(List<String> keys, String[] args){
        Number obj = redisUtils.execute(redisScript, keys, args);
        if (obj == null) {
            log.warn("Lua脚本返回值为null，无法转换为Integer");
            return null;
        }
        return obj.intValue();
    }
}
