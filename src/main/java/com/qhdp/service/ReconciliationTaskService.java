package com.qhdp.service;

/**
 * &#064;description:  对账执行 接口
 * &#064;author:  phoenix
 **/
public interface ReconciliationTaskService {
    
    void reconciliationTaskExecute();

    /**
     * 删除指定券的 Redis 库存键，触发按需重载。
     */
    void delRedisStock(Long voucherId);
}
