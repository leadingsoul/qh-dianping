package com.qhdp.delay.context;

import com.qhdp.constant.DelayQueueProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.redisson.api.RedissonClient;

/**
 * @description: 延迟队列配置信息
 * @author: phoenix
 **/
@Data
@AllArgsConstructor
public class DelayQueueBasePart {
    
    private final RedissonClient redissonClient;
    
    private final DelayQueueProperties delayQueueProperties;
}
