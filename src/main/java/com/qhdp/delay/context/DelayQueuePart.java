package com.qhdp.delay.context;

import com.qhdp.delay.core.ConsumerTask;
import lombok.Data;

/**
 * @description: 消息主题
 * @author: phoenix
 **/
@Data
public class DelayQueuePart {
    
    private final DelayQueueBasePart delayQueueBasePart;
 
    private final ConsumerTask consumerTask;
    
    public DelayQueuePart(DelayQueueBasePart delayQueueBasePart, ConsumerTask consumerTask){
        this.delayQueueBasePart = delayQueueBasePart;
        this.consumerTask = consumerTask;
    }
}
