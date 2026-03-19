package com.qhdp.servicelocker.info;

/**
 * @description: 处理失败抽象
 * @author: phoenix
 **/
public interface LockTimeOutHandler {
    
    /**
     * 处理
     * @param lockName 锁名
     * */
    void handler(String lockName);
}
