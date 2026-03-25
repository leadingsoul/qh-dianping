package com.qhdp.handler.handlerInterface;

import com.qhdp.enums.RateLimitScene;

/**
 * @description: 限流执行 接口
 * @author: 阿星不是程序员
 **/
public interface RateLimitHandler {
   
    void execute(Long voucherId, Long userId, RateLimitScene scene);
}
