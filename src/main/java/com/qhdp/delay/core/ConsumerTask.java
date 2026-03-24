package com.qhdp.delay.core;

/**
 * @description: 延迟队列 消费者接口
 * @author: phoenix
 **/
public interface ConsumerTask {
    
    void execute(String content);
  
    String topic();
}
